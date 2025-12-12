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
    private final PartRepo partRepo;
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

        // Get all parts for this submodule
        List<Part> parts = partRepo.findBySubmoduleIdAndIsActiveTrueOrderByPositionAsc(submoduleId);
        
        // Get lesson statuses for progress tracking
        Set<Long> doneLessons = profileLessonStatusRepo.findAllByIdProfileId(profileId).stream()
                .filter(pls -> pls.getStatus() == LessonStatus.DONE)
                .map(pls -> pls.getId().getLessonId())
                .collect(Collectors.toSet());

        List<PartListItemDTO> partDTOs = parts.stream()
                .map(part -> {
                    long totalLessons = lessonRepo.countByPartIdAndIsActiveTrue(part.getId());
                    
                    // Count completed lessons in this part
                    List<Lesson> partLessons = lessonRepo.findByPartIdAndIsActiveTrueOrderByPositionAsc(part.getId());
                    long completedLessons = partLessons.stream()
                            .filter(l -> doneLessons.contains(l.getId()))
                            .count();
                    
                    return new PartListItemDTO(
                            part.getId(),
                            part.getName(),
                            part.getSlug(),
                            part.getDescription(),
                            part.getPosition(),
                            (int) totalLessons,
                            (int) completedLessons
                    );
                })
                .filter(partDTO -> partDTO.getTotalLessons() > 0)  // Only show parts with lessons
                .toList();

        return new SubmoduleListDTO(s.getId(), s.getTitle(), s.getIntroText(), s.getPosition(), partDTOs);
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
    
    /**
     * Get a part with all its lessons and progress tracking
     */
    @Transactional(readOnly = true)
    public PartDTO getPart(Long profileId, String userId, Long partId) {
        Profile p = requireProfile(profileId, userId);
        Part part = partRepo.findById(partId)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        
        Module m = part.getSubmodule().getModule();
        checkPremiumAccess(p, m);
        
        // Get all lessons in this part
        List<Lesson> lessons = lessonRepo.findByPartIdAndIsActiveTrueOrderByPositionAsc(partId);
        
        // Get lesson statuses for progress tracking
        Set<Long> doneLessons = profileLessonStatusRepo.findAllByIdProfileId(profileId).stream()
                .filter(pls -> pls.getStatus() == LessonStatus.DONE)
                .map(pls -> pls.getId().getLessonId())
                .collect(Collectors.toSet());
        
        boolean unlockedGiven = false;
        List<LessonListItemDTO> lessonDTOs = new ArrayList<>();
        
        for (Lesson l : lessons) {
            LessonStatus status;
            if (doneLessons.contains(l.getId())) {
                status = LessonStatus.DONE;
            } else if (!unlockedGiven) {
                status = LessonStatus.UNLOCKED;
                unlockedGiven = true;
            } else {
                status = LessonStatus.LOCKED;
            }
            
            lessonDTOs.add(new LessonListItemDTO(
                    l.getId(), 
                    l.getTitle(), 
                    l.getHint(), 
                    l.getLessonType(), 
                    l.getPosition(), 
                    status
            ));
        }
        
        int totalLessons = lessonDTOs.size();
        int completedLessons = (int) lessonDTOs.stream()
                .filter(l -> l.status() == LessonStatus.DONE)
                .count();
        
        return new PartDTO(
                part.getId(),
                part.getName(),
                part.getSlug(),
                part.getDescription(),
                part.getPosition(),
                lessonDTOs,
                totalLessons,
                completedLessons
        );
    }

    /**
     * Get all assets for a submodule for offline caching
     * Returns S3 keys (not presigned URLs) for efficient batch downloading
     */
    @Transactional(readOnly = true)
    public SubmoduleAssetsDTO getSubmoduleAssets(Long profileId, String userId, Long submoduleId) {
        Profile p = requireProfile(profileId, userId);
        Submodule submodule = submoduleRepo.findById(submoduleId)
                .orElseThrow(() -> new EntityNotFoundException("Submodule not found"));
        
        Module m = submodule.getModule();
        checkPremiumAccess(p, m);
        
        // Get all parts for this submodule
        List<Part> parts = partRepo.findBySubmoduleIdAndIsActiveTrueOrderByPositionAsc(submoduleId);
        
        // Collect all assets from all lessons in all parts
        List<AssetInfoDTO> allAssets = new ArrayList<>();
        long estimatedSize = 0L;
        
        for (Part part : parts) {
            List<Lesson> lessons = lessonRepo.findByPartIdAndIsActiveTrueOrderByPositionAsc(part.getId());
            
            for (Lesson lesson : lessons) {
                // Get all screens for this lesson
                List<LessonScreen> screens = screenRepo.findByLessonIdOrderByPositionAsc(lesson.getId());
                
                for (LessonScreen screen : screens) {
                    // Parse the payload and extract all asset references
                    List<AssetInfoDTO> screenAssets = extractAssetsFromPayload(
                            screen.getPayload(), 
                            lesson.getId(), 
                            lesson.getTitle()
                    );
                    allAssets.addAll(screenAssets);
                    
                    // Estimate size (rough estimate: images ~500KB, audio ~1MB)
                    for (AssetInfoDTO asset : screenAssets) {
                        if ("AUDIO".equals(asset.kind())) {
                            estimatedSize += 1_000_000; // 1MB per audio
                        } else if ("IMAGE".equals(asset.kind())) {
                            estimatedSize += 500_000; // 500KB per image
                        } else {
                            estimatedSize += 100_000; // 100KB default
                        }
                    }
                }
            }
        }
        
        // Remove duplicates (same asset might be used in multiple lessons)
        List<AssetInfoDTO> uniqueAssets = allAssets.stream()
                .collect(Collectors.toMap(
                        AssetInfoDTO::assetId,
                        asset -> asset,
                        (existing, replacement) -> existing // Keep first occurrence
                ))
                .values()
                .stream()
                .toList();
        
        return new SubmoduleAssetsDTO(
                submoduleId,
                submodule.getTitle(),
                uniqueAssets,
                uniqueAssets.size(),
                estimatedSize
        );
    }
    
    /**
     * Extract asset information from a lesson screen payload
     */
    private List<AssetInfoDTO> extractAssetsFromPayload(String payloadJson, Long lessonId, String lessonTitle) {
        List<AssetInfoDTO> assets = new ArrayList<>();
        
        try {
            JsonNode root = om.readTree(payloadJson);
            extractAssetsRecursively(root, lessonId, lessonTitle, assets);
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Error parsing payload for lesson " + lessonId + ": " + e.getMessage());
        }
        
        return assets;
    }
    
    /**
     * Recursively extract assets from JSON payload
     */
    private void extractAssetsRecursively(JsonNode node, Long lessonId, String lessonTitle, List<AssetInfoDTO> assets) {
        if (node == null || node.isNull()) {
            return;
        }
        
        // Check if this object has an assetId
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            
            // Case 1: Direct asset reference { "assetId": 123 }
            if (obj.has("assetId") && obj.get("assetId").canConvertToLong()) {
                long assetId = obj.get("assetId").asLong();
                
                // Fetch asset details from database
                assetRepo.findById(assetId).ifPresent(asset -> {
                    String s3Key = asset.getUri();
                    
                    // Skip local app assets
                    if (!s3Key.startsWith("app://") && !s3Key.startsWith("assets/")) {
                        assets.add(new AssetInfoDTO(
                                asset.getId(),
                                s3Key,
                                asset.getKind().name(),
                                asset.getMime(),
                                asset.getChecksum(),
                                lessonId,
                                lessonTitle
                        ));
                    }
                });
            }
            
            // Case 2: S3 key fields (s3Key, s3AudioKey, s3ImageKey, etc.)
            obj.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                // Check for S3 key fields
                boolean isS3KeyField = (key.startsWith("s3") || key.contains("s3Key")) && value.isTextual();
                
                if (isS3KeyField) {
                    String s3Path = value.asText();
                    if (s3Service.isS3Key(s3Path)) {
                        // Create synthetic asset info for direct S3 keys
                        // (these don't have assetId in database)
                        assets.add(new AssetInfoDTO(
                                null, // no assetId for direct S3 keys
                                s3Path,
                                key.toLowerCase().contains("audio") ? "AUDIO" : "IMAGE",
                                detectMimeType(s3Path),
                                null, // no checksum
                                lessonId,
                                lessonTitle
                        ));
                    }
                } else {
                    // Recursively check child nodes
                    extractAssetsRecursively(value, lessonId, lessonTitle, assets);
                }
            });
        }
        
        // Handle arrays
        if (node.isArray()) {
            for (JsonNode item : node) {
                extractAssetsRecursively(item, lessonId, lessonTitle, assets);
            }
        }
    }
    
    /**
     * Detect MIME type from S3 key/filename
     */
    private String detectMimeType(String s3Key) {
        String lower = s3Key.toLowerCase();
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    /**
     * Generate presigned URL for an S3 key
     * Used by mobile app for offline asset downloading
     */
    public String generatePresignedUrl(Long profileId, String userId, String s3Key) {
        // Verify profile access
        requireProfile(profileId, userId);
        
        // Generate presigned URL
        if (s3Service.isS3Key(s3Key)) {
            return s3Service.generatePresignedUrl(s3Key);
        }
        
        return "";
    }
}
