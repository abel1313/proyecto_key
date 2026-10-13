#!/usr/bin/env python3
"""Skill revision-colores. Busca en los .scss/.css comentarios /* ... */ que se "comieron"
declaraciones (un comentario sin cerrar se traga todo hasta el siguiente */ sin dar error).
Pasó el 2026-10-13: dos comentarios sin cerrar en el bloque de noche de styles.scss borraban
--sb-text-active, --sb-bg-item y los colores de las tarjetas, y el menú salía azul/negro.
Uso: python3 comentarios-sin-cerrar.py <carpeta src del front>"""
import re, sys, pathlib

raiz = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else 'src')
hallazgos = 0
for f in list(raiz.rglob('*.scss')) + list(raiz.rglob('*.css')):
    s = f.read_text(encoding='utf-8', errors='replace')
    for m in re.finditer(r'/\*(.*?)\*/', s, flags=re.S):
        cuerpo = m.group(1)
        # Una declaración de propiedad o de variable dentro del comentario = probablemente tragada.
        decl = re.findall(r'^\s*(--[\w-]+|[a-z-]+)\s*:\s*[^;{}]+;\s*$', cuerpo, flags=re.M)
        if decl or '/*' in cuerpo:
            linea = s[:m.start()].count('\n') + 1
            hallazgos += 1
            print(f'{f}:{linea}: comentario con {len(decl)} declaración(es) adentro'
                  + (' y otro "/*" adentro (comentario sin cerrar antes)' if '/*' in cuerpo else '')
                  + f' -> {", ".join(decl[:4])}')
print(f'{hallazgos} comentario(s) sospechoso(s)')
sys.exit(1 if hallazgos else 0)
