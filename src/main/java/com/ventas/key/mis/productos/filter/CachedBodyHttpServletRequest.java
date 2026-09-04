package com.ventas.key.mis.productos.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Envoltorio que lee el body UNA vez a memoria y lo deja disponible para leerse cuantas veces
 * haga falta -- necesario porque {@link InputSanitizationFilter} tiene que inspeccionar el JSON
 * completo ANTES de que el controlador lo reciba, pero el InputStream de un HttpServletRequest
 * solo se puede consumir una vez. Sin esto, el @RequestBody del controlador llegaria vacio
 * despues de que el filtro ya lo hubiera leido.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cuerpoCacheado;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cuerpoCacheado = StreamUtils.copyToByteArray(request.getInputStream());
    }

    public byte[] getCuerpoCacheado() {
        return cuerpoCacheado;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cuerpoCacheado);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // No hace falta -- lectura sincrona sobre bytes ya en memoria.
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
