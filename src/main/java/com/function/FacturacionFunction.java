package com.function;

import java.util.Optional;
import java.util.logging.Logger;

import com.function.model.ApiResponse;
import com.function.model.FacturacionRequest;
import com.function.model.FacturacionResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class FacturacionFunction {
    @FunctionName("FacturacionPrototipo")
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "facturacion/generar"
        ) HttpRequestMessage<Optional<FacturacionRequest>> request,
        final ExecutionContext context
    ) {
        Logger log = context.getLogger();

        try {           
            FacturacionRequest body = request.getBody().orElse(null);

            if (body == null) {
                return jsonResponse(
                    request,
                    HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(false, "El body no puede venir vacío.", null)
                );
            }

            if (isNullOrEmpty(body.getClienteId())
                    || isNullOrEmpty(body.getMascotaId())
                    || isNullOrEmpty(body.getDetalle())
                    || body.getCantidad() == null
                    || body.getPrecioUnitario() == null) {

                return jsonResponse(
                    request,
                    HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(false,
                        "Faltan campos obligatorios: clienteId, mascotaId, detalle, cantidad, precioUnitario.",
                        null)
                );
            }

            if (body.getCantidad() <= 0) {
                return jsonResponse(
                    request,
                    HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(false, "La cantidad debe ser mayor a 0.", null)
                );                        
            }

            if (body.getPrecioUnitario() < 0) {
                return jsonResponse(
                    request,
                    HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(false, "El precioUnitario no puede ser negativo.", null)
                );                        
            }

            double descuento = body.getDescuento() != null ? body.getDescuento() : 0.0;
            double subtotal = body.getCantidad() * body.getPrecioUnitario();
            double total = subtotal - descuento;

            if (total < 0) {
                total = 0;
            }

            FacturacionResponse response = new FacturacionResponse();
            response.setClienteId(body.getClienteId());
            response.setMascotaId(body.getMascotaId());
            response.setDetalle(body.getDetalle());
            response.setCantidad(body.getCantidad());
            response.setPrecioUnitario(body.getPrecioUnitario());
            response.setSubtotal(subtotal);
            response.setDescuento(descuento);
            response.setTotal(total);
            response.setEstado("FACTURA_GENERADA");

            log.info("=== PROTOTIPO FACTURACION ===");
            log.info("Cliente    : " + body.getClienteId());
            log.info("Mascota    : " + body.getMascotaId());
            log.info("Detalle    : " + body.getDetalle());
            log.info("Subtotal   : " + subtotal);
            log.info("Descuento  : " + descuento);
            log.info("Total      : " + total);

             return jsonResponse(
                request,
                HttpStatus.OK,
                new ApiResponse<>(true, "Factura generada correctamente.", response)
            );

        } catch (Exception e) {
            log.severe("Error en la función de facturación: " + e.getMessage());

            return jsonResponse(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                new ApiResponse<>(false, "Error procesando la facturación.", null)
            );
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private HttpResponseMessage jsonResponse(
        HttpRequestMessage<?> request,
        HttpStatus status,
        Object body
    ) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }
}
