package ru.itmo.ticketing.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.NotFoundException;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    public Category create(String name) {
        String normalized = name.trim();
        if (categories.existsByNameIgnoreCase(normalized)) {
            throw new ConflictException("Category '" + normalized + "' already exists");
        }
        return categories.save(new Category(normalized));
    }

    public Category rename(Long id, String name) {
        Category category = get(id);
        category.setName(name.trim());
        return category;
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    }

    @Transactional(readOnly = true)
    public List<Category> list() {
        return categories.findAll();
    }

    public void delete(Long id) {
        categories.delete(get(id));
    }
}
