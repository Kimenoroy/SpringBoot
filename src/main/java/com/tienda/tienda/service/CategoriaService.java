package com.tienda.tienda.service;

import com.tienda.tienda.dto.CategoriaRequest;
import com.tienda.tienda.dto.CategoriaResponse;
import com.tienda.tienda.exception.BusinessException;
import com.tienda.tienda.exception.ResourceNotFoundException;
import com.tienda.tienda.model.Categoria;
import com.tienda.tienda.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una categoría con el nombre: " + request.getNombre());
        }
        Categoria categoria = new Categoria(request.getNombre(), request.getDescripcion());
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarPorId(id);
        categoriaRepository.findByNombre(request.getNombre())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new BusinessException("Ya existe una categoría con el nombre: " + request.getNombre());
                });
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        if (!categoria.getProductos().isEmpty()) {
            throw new BusinessException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }
}
