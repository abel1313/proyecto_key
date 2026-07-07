



variantes/v1/admin/habilitar-lote
1.- Estoy revisando la forma de de habilitar las variantes que segun la formade des habilitar a las variantes
debe de ser a las variantes como tal y veo que si se ejecutar correcto el codigo y aparece el mensaje que las variantes se des habilitaron correctamente
pero en la base de datos no se actualizan las varibles
request
curl 'https://backend.novedades-jade.com.mx/mis-productos/variantes/v1/admin/habilitar-lote' \
-X 'PUT' \
-H 'Accept: application/json, text/plain, */*' \
-H 'Accept-Language: es-419,es;q=0.7' \
-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJDTElFTlRFU19FTElNSU5BUiIsIkdBU1RPU19HRVNUSU9OQVIiLCJQUk9EVUNUT1NfTEVFUiIsIlBFRElET1NfRUxJTUlOQVIiLCJWRU5UQVNfQ1JFQVIiLCJWQVJJQU5URVNfRURJVEFSIiwiUFJPRFVDVE9TX0VMSU1JTkFSIiwiVkFSSUFOVEVTX0xFRVIiLCJDTElFTlRFU19FRElUQVIiLCJWQVJJQU5URVNfQ1JFQVIiLCJWRU5UQVNfTEVFUiIsIlBFRElET1NfQ1JFQVIiLCJSSUZBU19HRVNUSU9OQVIiLCJQUk9EVUNUT1NfQ1JFQVIiLCJQQUdPU19MRUVSIiwiTVBfQ09CUkFSIiwiQ0xJRU5URVNfTEVFUiIsIlJPTEVfQURNSU4iLCJVU1VBUklPU19HRVNUSU9OQVIiLCJQRURJRE9TX0VESVRBUiIsIlBST0RVQ1RPU19FRElUQVIiLCJQRURJRE9TX0xFRVIiLCJDTElFTlRFU19DUkVBUiIsIklNQUdFTkVTX0dFU1RJT05BUiJdLCJpZFVzdWFyaW8iOjQzLCJqdGkiOiI2ZDk5OGRkNC04OTFkLTRlZmQtYjMwYi02MmIxOTcyYzQwODgiLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc4MzM2NDkwNSwiZXhwIjoxNzgzMzY1ODA1fQ.LGCUOc2GBchhJy3OkABdzBGStIAqEztF1jper7al0_Q' \
-H 'Connection: keep-alive' \
-H 'Content-Type: application/json' \
-H 'Origin: https://shop.novedades-jade.com.mx' \
-H 'Referer: https://shop.novedades-jade.com.mx/' \
-H 'Sec-Fetch-Dest: empty' \
-H 'Sec-Fetch-Mode: cors' \
-H 'Sec-Fetch-Site: same-site' \
-H 'Sec-GPC: 1' \
-H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36' \
-H 'sec-ch-ua: "Brave";v="147", "Not.A/Brand";v="8", "Chromium";v="147"' \
-H 'sec-ch-ua-mobile: ?0' \
-H 'sec-ch-ua-platform: "macOS"' \
--data-raw '{"ids":[1,9,10,11,12,13,26,28,30,31],"habilitar":false}'

response
{
"mensaje": "La peticion fue exitosa",
"code": 200,
"data": "Variantes deshabilitadas correctamente",
"lista": null
}

2.- Cuando se hace la busqueda por codigo o nombre, y selecciono una opcion por ejemplo  no habilitado o sin stock
con stock, o con imagenes, pues no se envia el filtro por el nombre y solo la opcion seleccionada?
para productos si se esta des habilitando y habilitando, pero para la parte de variantes no las des habilita


