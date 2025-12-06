package rotld.apscrm.api.v1.logopedy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.logopedy.dto.*;
import rotld.apscrm.api.v1.logopedy.entities.*;
import rotld.apscrm.api.v1.logopedy.entities.Module;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.enums.TargetAudience;
import rotld.apscrm.api.v1.logopedy.repository.*;
import rotld.apscrm.exception.PremiumRequiredException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ModuleRepo moduleRepo;
    private final SubmoduleRepo submoduleRepo;
    private final LessonRepo lessonRepo;
    private final LessonScreenRepo screenRepo;
    private final ProfileRepo profileRepo;
    private final ProfileLessonStatusRepo profileLessonStatusRepo;
    private final AssetRepo assetRepo;
    private final S3Service s3Service;

    private final com.fasterxml.jackson.databind.ObjectMapper om;

    private Profile requireProfile(Long profileId, String userId) {
        return profileRepo.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
    }

    private void checkPremiumAccess(Profile profile, Module module) {
        if (module.isPremium() && !profile.getUser().getIsPremium()) {
            throw new PremiumRequiredException();
        }
    }

    public List<ModuleDTO> listModules(Long profileId, String userId, String targetAudienceParam) {
        Profile p = requireProfile(profileId, userId);
        
        // Get all active modules
        var modules = moduleRepo.findAllByIsActiveTrueOrderByPositionAsc().stream();
        
        // Filter by targetAudience if provided
        if (targetAudienceParam != null && !targetAudienceParam.isBlank()) {
            try {
                TargetAudience targetAudience = TargetAudience.valueOf(targetAudienceParam.toUpperCase());
                modules = modules.filter(m -> m.getTargetAudience() == targetAudience);
            } catch (IllegalArgumentException e) {
                // Invalid targetAudience value, ignore and return all modules
            }
        }
        
        return modules
                .map(m -> new ModuleDTO(
                        m.getId(), m.getTitle(), m.getIntroText(), m.getPosition(), m.isPremium(), 
                        m.getTargetAudience(), null))
                .toList();
    }

    public ModuleDTO getModule(Long profileId, String userId, Long moduleId) {
        Profile p = requireProfile(profileId, userId);
        Module m = moduleRepo.findById(moduleId).orElseThrow(() -> new EntityNotFoundException("Module"));
        checkPremiumAccess(p, m);

        List<SubmoduleDTO> subs = submoduleRepo.findByModuleIdOrderByPositionAsc(moduleId)
                .stream().map(s -> new SubmoduleDTO(s.getId(), s.getTitle(), s.getIntroText(), s.getPosition(), null))
                .toList();

        return new ModuleDTO(m.getId(), m.getTitle(), m.getIntroText(), m.getPosition(), m.isPremium(), 
                m.getTargetAudience(), subs);
    }

    public SubmoduleListDTO getSubmodule(Long profileId, String userId, Long submoduleId) {
        Profile p = requireProfile(profileId, userId);
        Submodule s = submoduleRepo.findById(submoduleId).orElseThrow(() -> new EntityNotFoundException("Submodule"));
        Module m = s.getModule();
        checkPremiumAccess(p, m);

        List<LessonListItemDTO> lessons = lessonRepo.findBySubmoduleIdOrderByPositionAsc(submoduleId)
                .stream()
                .map(l -> new LessonListItemDTO(
                        l.getId(), l.getTitle(), l.getHint(), l.getLessonType(), l.getPosition(), null
                ))
                .toList();

        return new SubmoduleListDTO(s.getId(), s.getTitle(), s.getIntroText(), s.getPosition(), lessons);
    }

    public LessonPlayDTO getLesson(Long profileId, String userId, Long lessonId) {
        Profile p = requireProfile(profileId, userId);
        Lesson l = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson with id %s not found.".formatted(lessonId)));
        checkPremiumAccess(p, l.getSubmodule().getModule());

        var screens = screenRepo.findByLessonIdOrderByPositionAsc(lessonId).stream()
                .map(sc -> new ScreenDTO(
                        sc.getId(),
                        sc.getScreenType(),
                        parseAndResolve(sc.getPayload()),   // <— AICI!
                        sc.getPosition()
                ))
                .toList();

        return new LessonPlayDTO(
                l.getId(), l.getTitle(), l.getHint(), l.getLessonType(), l.getPosition(), screens
        );
    }

    private Map<String, Object> parseAndResolve(String raw) {
        try {
            JsonNode root = om.readTree(raw);
            JsonNode resolved = resolveAssets(root);
            return om.convertValue(resolved, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of(); // fallback safe
        }
    }

    private JsonNode resolveAssets(JsonNode node) {
        if (node == null) return NullNode.getInstance();

        // 1) Obiect: poate conține assetId sau copii cu assetId
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;

            // Caz direct: { "assetId": 501 }
            if (obj.has("assetId") && obj.get("assetId").canConvertToLong()) {
                long id = obj.get("assetId").asLong();
                return assetNodeFor(id); // înlocuim tot obiectul cu {uri, kind, mime}
            }

            // Recursiv pe copii, dar verifică și câmpurile S3
            ObjectNode out = om.createObjectNode();
            obj.fields().forEachRemaining(e -> {
                String key = e.getKey();
                JsonNode value = e.getValue();
                
                // Verifică dacă este un câmp S3 key (orice câmp care conține "s3" și "Key" sau începe cu "s3")
                // Acceptă: s3Key, s3AudioKey, s3ImageKey, s3AudioQuestionKey, etc.
                boolean isS3KeyField = (key.startsWith("s3") || key.contains("s3Key")) && value.isTextual();
                
                if (isS3KeyField) {
                    String s3Path = value.asText();
                    // Generează presigned URL dacă este un S3 key valid
                    if (s3Service.isS3Key(s3Path)) {
                        String presignedUrl = s3Service.generatePresignedUrl(s3Path);
                        out.put(key, presignedUrl);
                    } else {
                        out.set(key, value);
                    }
                } else {
                    // Rezolvă recursiv pentru alte câmpuri
                    out.set(key, resolveAssets(value));
                }
            });
            return out;
        }

        // 2) Array: rezolvă fiecare element
        if (node.isArray()) {
            ArrayNode arr = om.createArrayNode();
            for (JsonNode it : node) arr.add(resolveAssets(it));
            return arr;
        }

        // 3) Altfel (string/număr/bool/null) – returnează neschimbat
        return node;
    }

    /** Transformă assetId în { uri, kind, mime }.
     *  Pentru Flutter assets locale, scoatem prefixul "app://".
     *  Pentru S3 assets, generăm pre-signed URLs pentru acces securizat.
     */
    private JsonNode assetNodeFor(long id) {
        var a = assetRepo.findById(id).orElse(null);
        ObjectNode out = om.createObjectNode();
        if (a == null) {
            out.put("uri", "");
            out.put("kind", "");
            out.put("mime", "");
            return out;
        }
        
        String uri = a.getUri(); // ex: app://assets/images/soare.png OR modules/1/submodules/1/lessons/1/image.jpg
        
        // Handle local app assets
        if (uri.startsWith("app://")) {
            uri = uri.substring("app://".length()); // -> assets/images/soare.png
        }
        // Handle S3 assets - generate pre-signed URL
        else if (s3Service.isS3Key(uri)) {
            uri = s3Service.generatePresignedUrl(uri);
        }
        
        out.put("uri", uri);
        out.put("kind", a.getKind().name());   // IMAGE / AUDIO, etc
        out.put("mime", a.getMime());
        return out;
    }

    private Object json(String raw) {
        try {
            return om.readTree(raw);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Transactional(readOnly = true)
    public List<LessonListItemDTO> submoduleLessonsWithProgress(Long profileId, Long submoduleId, String userId) {
        requireProfile(profileId, userId);

        var lessons = lessonRepo.findBySubmoduleIdOrderByPositionAsc(submoduleId);

        // status DONE pentru lecțiile terminate
        Set<Long> done = profileLessonStatusRepo.findAllByIdProfileId(profileId).stream()
                .filter(pls -> pls.getStatus() == LessonStatus.DONE)
                .map(pls -> pls.getId().getLessonId())
                .collect(Collectors.toSet());

        boolean unlockedGiven = false;
        List<LessonListItemDTO> out = new ArrayList<>();

        for (var l : lessons) {
            String s;
            if (done.contains(l.getId())) {
                s = "DONE";
            } else if (!unlockedGiven) {
                s = "UNLOCKED";   // prima lecție încă nedone → deblocat
                unlockedGiven = true;
            } else {
                s = "LOCKED";
            }

            out.add(new LessonListItemDTO(
                    l.getId(), l.getTitle(), l.getHint(), l.getLessonType(), l.getPosition(), LessonStatus.valueOf(s)
            ));
        }
        return out;
    }
}
