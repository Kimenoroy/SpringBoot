package com.tienda.tienda.service;

import com.tienda.tienda.dto.*;
import com.tienda.tienda.exception.BusinessException;
import com.tienda.tienda.exception.ResourceNotFoundException;
import com.tienda.tienda.model.*;
import com.tienda.tienda.repository.PedidoRepository;
import com.tienda.tienda.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;
    private final ProductoService productoService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         ClienteService clienteService,
                         ProductoService productoService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.clienteService = clienteService;
        this.productoService = productoService;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        Cliente cliente = clienteService.buscarPorId(request.getClienteId());

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);

        BigDecimal total = BigDecimal.ZERO;

        for (DetallePedidoRequest detalleRequest : request.getDetalles()) {
            Producto producto = productoService.buscarPorId(detalleRequest.getProductoId());

            if (!producto.getActivo()) {
                throw new BusinessException("El producto '" + producto.getNombre() + "' no está disponible");
            }
            if (producto.getStock() < detalleRequest.getCantidad()) {
                throw new BusinessException("Stock insuficiente para '" + producto.getNombre()
                        + "'. Disponible: " + producto.getStock());
            }

            producto.setStock(producto.getStock() - detalleRequest.getCantidad());
            productoRepository.save(producto);

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(detalleRequest.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalleRequest.getCantidad())));

            total = total.add(detalle.getSubtotal());
            pedido.agregarDetalle(detalle);
        }

        pedido.setTotal(total);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        EstadoPedido estadoActual = pedido.getEstado();

        if (estadoActual == EstadoPedido.ENTREGADO || estadoActual == EstadoPedido.CANCELADO) {
            throw new BusinessException("No se puede modificar un pedido " + estadoActual.name().toLowerCase());
        }
        if (nuevoEstado == EstadoPedido.CANCELADO) {
            devolverStock(pedido);
        }

        pedido.setEstado(nuevoEstado);
        return toResponse(pedidoRepository.save(pedido));
    }

    private void devolverStock(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
        }
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        PedidoResponse response = new PedidoResponse();
        response.setId(pedido.getId());
        response.setClienteId(pedido.getCliente().getId());
        response.setClienteNombre(pedido.getCliente().getNombre() + " " + pedido.getCliente().getApellido());
        response.setFechaPedido(pedido.getFechaPedido());
        response.setEstado(pedido.getEstado().name());
        response.setTotal(pedido.getTotal());
        response.setDetalles(pedido.getDetalles().stream()
                .map(this::toDetalleResponse)
                .toList());
        return response;
    }

    private DetallePedidoResponse toDetalleResponse(DetallePedido detalle) {
        DetallePedidoResponse response = new DetallePedidoResponse();
        response.setId(detalle.getId());
        response.setProductoId(detalle.getProducto().getId());
        response.setProductoNombre(detalle.getProducto().getNombre());
        response.setCantidad(detalle.getCantidad());
        response.setPrecioUnitario(detalle.getPrecioUnitario());
        response.setSubtotal(detalle.getSubtotal());
        return response;
    }
}
