package ru.itmo.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.NotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tags;

    public Tag create(String name) {
        String normalized = name.trim();
        if (tags.existsByNameIgnoreCase(normalized)) {
            throw new ConflictException("Tag '" + normalized + "' already exists");
        }
        return tags.save(new Tag(normalized));
    }

    public Tag rename(Long id, String name) {
        Tag tag = get(id);
        tag.setName(name.trim());
        return tag;
    }

    @Transactional(readOnly = true)
    public Tag get(Long id) {
        return tags.findById(id).orElseThrow(() -> new NotFoundException("Tag", id));
    }

    @Transactional(readOnly = true)
    public Page<Tag> list(Pageable pageable) {
        return tags.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Tag> getAll(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Tag> found = tags.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            Set<Long> missing = new HashSet<>(ids);
            found.forEach(t -> missing.remove(t.getId()));
            throw new NotFoundException("Tags not found: " + missing);
        }
        return found;
    }

    public void delete(Long id) {
        tags.delete(get(id));
    }
}
