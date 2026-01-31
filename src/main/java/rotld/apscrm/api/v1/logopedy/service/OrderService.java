package rotld.apscrm.api.v1.logopedy.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.logopedy.entities.*;
import rotld.apscrm.api.v1.logopedy.entities.Module;
import rotld.apscrm.api.v1.logopedy.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing entity ordering using JSON arrays in parent entities.
 * Replaces position-based ordering with array-index ordering.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final ModuleRepo moduleRepo;
    private final SubmoduleRepo submoduleRepo;
    private final PartRepo partRepo;
    private final LessonRepo lessonRepo;
    private final LessonScreenRepo screenRepo;

    // ============== FETCH ORDERED ENTITIES ==============

    /**
     * Get all active modules ordered by submoduleOrder array.
     * Falls back to position-based ordering if array is empty.
     */
    public List<Module> getOrderedModules() {
        return moduleRepo.findAllByIsActiveTrueOrderByPositionAsc();
    }

    /**
     * Get all active submodules for a module, ordered by the module's submoduleOrder array.
     * Falls back to position-based ordering if array is empty.
     */
    public List<Submodule> getOrderedSubmodulesForModule(Long moduleId) {
        Module module = moduleRepo.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("Module not found"));
        
        List<Long> order = module.getSubmoduleOrder();
        if (order == null || order.isEmpty()) {
            // Fallback to position-based ordering
            return submoduleRepo.findAllByModule_IdOrderByPositionAsc(moduleId);
        }
        
        List<Submodule> submodules = submoduleRepo.findAllByModule_IdAndIsActiveTrue(moduleId);
        return applyOrder(order, submodules, Submodule::getId);
    }

    /**
     * Get all active parts for a submodule, ordered by the submodule's partOrder array.
     * Falls back to position-based ordering if array is empty.
     */
    public List<Part> getOrderedPartsForSubmodule(Long submoduleId) {
        Submodule submodule = submoduleRepo.findById(submoduleId)
                .orElseThrow(() -> new EntityNotFoundException("Submodule not found"));
        
        List<Long> order = submodule.getPartOrder();
        if (order == null || order.isEmpty()) {
            // Fallback to position-based ordering
            return partRepo.findBySubmoduleIdAndIsActiveTrueOrderByPositionAsc(submoduleId);
        }
        
        List<Part> parts = partRepo.findBySubmoduleIdAndIsActiveTrue(submoduleId);
        return applyOrder(order, parts, Part::getId);
    }

    /**
     * Get all active lessons for a part, ordered by the part's lessonOrder array.
     * Falls back to position-based ordering if array is empty.
     */
    public List<Lesson> getOrderedLessonsForPart(Long partId) {
        Part part = partRepo.findById(partId)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        
        List<Long> order = part.getLessonOrder();
        if (order == null || order.isEmpty()) {
            // Fallback to position-based ordering
            return lessonRepo.findByPartIdAndIsActiveTrueOrderByPositionAsc(partId);
        }
        
        List<Lesson> lessons = lessonRepo.findByPartIdAndIsActiveTrue(partId);
        return applyOrder(order, lessons, Lesson::getId);
    }

    /**
     * Get all screens for a lesson, ordered by the lesson's screenOrder array.
     * Falls back to position-based ordering if array is empty.
     */
    public List<LessonScreen> getOrderedScreensForLesson(Long lessonId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found"));
        
        List<Long> order = lesson.getScreenOrder();
        if (order == null || order.isEmpty()) {
            // Fallback to position-based ordering
            return screenRepo.findByLessonIdOrderByPositionAsc(lessonId);
        }
        
        List<LessonScreen> screens = screenRepo.findAllById(order);
        return applyOrder(order, screens, LessonScreen::getId);
    }

    /**
     * Get all active lessons for a submodule (across all parts), ordered.
     * Falls back to position-based ordering if arrays are empty.
     */
    public List<Lesson> getOrderedLessonsForSubmodule(Long submoduleId) {
        List<Part> orderedParts = getOrderedPartsForSubmodule(submoduleId);
        List<Lesson> allLessons = new ArrayList<>();
        
        for (Part part : orderedParts) {
            allLessons.addAll(getOrderedLessonsForPart(part.getId()));
        }
        
        return allLessons;
    }

    // ============== HELPER METHODS ==============

    /**
     * Apply ordering from an ID list to a collection of entities.
     * Entities not in the order list are appended at the end.
     */
    private <T> List<T> applyOrder(List<Long> order, List<T> entities, Function<T, Long> idExtractor) {
        if (order == null || order.isEmpty()) {
            return entities;
        }
        
        Map<Long, T> byId = entities.stream()
                .collect(Collectors.toMap(idExtractor, e -> e, (a, b) -> a));
        
        List<T> ordered = new ArrayList<>();
        
        // Add entities in order
        for (Long id : order) {
            T entity = byId.remove(id);
            if (entity != null) {
                ordered.add(entity);
            }
        }
        
        // Add any remaining entities (not in order array) at the end
        ordered.addAll(byId.values());
        
        return ordered;
    }

    /**
     * Find the index of a lesson in its part's order array.
     * Returns -1 if not found or if using fallback ordering.
     */
    public int getLessonIndexInPart(Long lessonId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found"));
        
        Part part = lesson.getPart();
        List<Long> order = part.getLessonOrder();
        
        if (order == null || order.isEmpty()) {
            // Fallback: find index by position
            List<Lesson> lessons = lessonRepo.findByPartIdAndIsActiveTrueOrderByPositionAsc(part.getId());
            for (int i = 0; i < lessons.size(); i++) {
                if (lessons.get(i).getId().equals(lessonId)) {
                    return i;
                }
            }
            return -1;
        }
        
        return order.indexOf(lessonId);
    }

    /**
     * Get the next lesson in a part after the given lesson.
     * Returns null if this is the last lesson.
     */
    public Lesson getNextLessonInPart(Long lessonId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found"));
        
        List<Lesson> orderedLessons = getOrderedLessonsForPart(lesson.getPart().getId());
        
        for (int i = 0; i < orderedLessons.size() - 1; i++) {
            if (orderedLessons.get(i).getId().equals(lessonId)) {
                return orderedLessons.get(i + 1);
            }
        }
        
        return null; // Last lesson in part
    }

    /**
     * Get the first lesson in a part.
     * Returns null if part has no lessons.
     */
    public Lesson getFirstLessonInPart(Long partId) {
        List<Lesson> lessons = getOrderedLessonsForPart(partId);
        return lessons.isEmpty() ? null : lessons.get(0);
    }
}

