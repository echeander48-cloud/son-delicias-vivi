/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.sondeliciasvivi;

/**
 *
 * @author echea
 */
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class RestauranteWeb {

    // Contraseña única del administrador
    private static final String PASSWORD_ADMIN = "admin123"; // Puedes cambiarla aquí

    // Almacenamiento en memoria de los menús por día o fecha
    private static final Map<String, String> menuDiario = new LinkedHashMap<>();


    public static void main(String[] args) {
        try {
            HttpServer servidor = HttpServer.create(
                new InetSocketAddress(8080), 0
            );

            // 1. Página Principal (Pública para los clientes)
            servidor.createContext("/", (HttpExchange intercambio) -> {
                if (intercambio.getRequestURI().getPath().startsWith("/admin")) {
                    intercambio.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] respuesta = crearPaginaPrincipal().getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                intercambio.sendResponseHeaders(200, respuesta.length);
                try (var salida = intercambio.getResponseBody()) {
                    salida.write(respuesta);
                }
            });

            // 2. Subpágina de Administración (/admin)
            servidor.createContext("/admin", (HttpExchange intercambio) -> {
                String query = intercambio.getRequestURI().getQuery();
                boolean autenticado = query != null && query.contains("key=" + PASSWORD_ADMIN);

                byte[] respuesta;
                if (!autenticado) {
                    respuesta = crearPaginaLogin().getBytes(StandardCharsets.UTF_8);
                } else {
                    respuesta = crearPaginaAdmin().getBytes(StandardCharsets.UTF_8);
                }

                intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                intercambio.sendResponseHeaders(200, respuesta.length);
                try (var salida = intercambio.getResponseBody()) {
                    salida.write(respuesta);
                }
            });

            // 3. Validar login inicial
            servidor.createContext("/admin/login", (HttpExchange intercambio) -> {
                if ("POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                    String body = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parsearParametros(body);
                    String pass = URLDecoder.decode(params.getOrDefault("password", ""), StandardCharsets.UTF_8);

                    if (PASSWORD_ADMIN.equals(pass)) {
                        intercambio.getResponseHeaders().set("Location", "/admin?key=" + PASSWORD_ADMIN);
                        intercambio.sendResponseHeaders(303, -1);
                        return;
                    }
                }
                intercambio.getResponseHeaders().set("Location", "/admin?error=1");
                intercambio.sendResponseHeaders(303, -1);
            });

            // 4. Procesar Guardar / Modificar Menú
            servidor.createContext("/admin/guardar", (HttpExchange intercambio) -> {
                if ("POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                    String body = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parsearParametros(body);
                    
                    String dia = URLDecoder.decode(params.getOrDefault("dia", ""), StandardCharsets.UTF_8);
                    String plato = URLDecoder.decode(params.getOrDefault("plato", ""), StandardCharsets.UTF_8);
                    
                    if (!dia.isEmpty() && !plato.isEmpty()) {
                        menuDiario.put(dia, plato);
                    }
                }
                intercambio.getResponseHeaders().set("Location", "/admin?key=" + PASSWORD_ADMIN);
                intercambio.sendResponseHeaders(303, -1);
            });

            // 5. Procesar Eliminar Menú
            servidor.createContext("/admin/eliminar", (HttpExchange intercambio) -> {
                if ("POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                    String body = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parsearParametros(body);
                    
                    String dia = URLDecoder.decode(params.getOrDefault("dia", ""), StandardCharsets.UTF_8);
                    if (!dia.isEmpty()) {
                        menuDiario.remove(dia);
                    }
                }
                intercambio.getResponseHeaders().set("Location", "/admin?key=" + PASSWORD_ADMIN);
                intercambio.sendResponseHeaders(303, -1);
            });

            servidor.start();

            System.out.println("========================================");
            System.out.println(" RESTAURANTE WEB INICIADO");
            System.out.println(" Página Clientes: http://localhost:8080");
            System.out.println(" Panel Dueño:     http://localhost:8080/admin");
            System.out.println("========================================");

        } catch (IOException e) {
            System.out.println("Error al iniciar: " + e.getMessage());
        }
    }

    private static Map<String, String> parsearParametros(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String param : query.split("&")) {
            String[] par = param.split("=");
            if (par.length > 1) {
                params.put(par[0], par[1]);
            } else if (par.length == 1) {
                params.put(par[0], "");
            }
        }
        return params;
    }

    // ==========================================
    // PÁGINA PRINCIPAL PÚBLICA
    // ==========================================
    public static String crearPaginaPrincipal() {
        StringBuilder htmlMenuDiario = new StringBuilder();

        for (Map.Entry<String, String> entry : menuDiario.entrySet()) {
            htmlMenuDiario.append("""
                <div class="dia-item">
                    <h3>%s</h3>
                    <p>%s</p>
                </div>
            """.formatted(entry.getKey(), entry.getValue()));
        }

        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Son Delicias Vivi | Restaurante</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; scroll-behavior: smooth; }
                body { font-family: Arial, sans-serif; background: #f9f9f9; color: #333333; }
                header { background: #ffffff; color: #222222; padding: 20px 8%; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 15px; border-bottom: 1px solid #e0e0e0; box-shadow: 0 2px 5px rgba(0,0,0,0.03); }
                .logo { font-size: 24px; font-weight: bold; color: #222222; text-decoration: none; }
                .logo span { color: #ff4d4d; }
                nav a { color: #555555; text-decoration: none; margin-left: 20px; font-size: 15px; font-weight: bold; transition: color 0.3s; }
                nav a:hover { color: #ff4d4d; }
                .hero { min-height: 450px; padding: 70px 8%; display: flex; align-items: center; justify-content: space-between; background: #ffffff; color: #222222; gap: 40px; border-bottom: 1px solid #e5e5e5; }
                .hero-content { flex: 1; max-width: 550px; }
                .hero h1 { font-size: clamp(36px, 5vw, 58px); margin-bottom: 20px; line-height: 1.1; color: #111111; }
                .hero p { font-size: 18px; line-height: 1.7; margin-bottom: 28px; color: #666666; }
                .hero-image { flex: 1; display: flex; justify-content: center; align-items: center; }
                .hero-image img { max-width: 100%; max-height: 380px; object-fit: contain; mix-blend-mode: multiply; }
                .boton { display: inline-block; background: #ff4d4d; color: white; padding: 14px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; border: none; cursor: pointer; transition: transform 0.2s, background 0.2s; }
                .boton:hover { background: #e63946; transform: scale(1.05); }
                .seccion { padding: 55px 8%; }
                .titulo { text-align: center; font-size: 32px; margin-bottom: 12px; color: #111111; }
                .subtitulo { text-align: center; color: #777777; margin-bottom: 35px; }
                .categorias { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; margin-bottom: 30px; }
                .categoria { padding: 10px 18px; border-radius: 25px; background: #e9ecef; color: #333333; text-decoration: none; font-weight: bold; transition: all 0.3s; }
                .categoria:hover { background: #ced4da; }
                .productos { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 25px; }
                .producto { background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); transition: transform .2s; border: 1px solid #eaeaea; }
                .producto:hover { transform: translateY(-5px); }
                .producto img { width: 100%; height: 210px; object-fit: cover; }
                .detalle { padding: 20px; }
                .detalle h3 { margin-bottom: 10px; font-size: 20px; color: #222222; }
                .detalle p { color: #666666; line-height: 1.5; min-height: 45px; margin-bottom: 15px; }
                .precio { color: #ff4d4d; font-size: 22px; font-weight: bold; margin-bottom: 15px; }
                .detalle .boton { width: 100%; text-align: center; }
                .menu-diario-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
                .dia-item { background: #ffffff; padding: 20px; border-radius: 12px; border-left: 5px solid #ff4d4d; box-shadow: 0 3px 10px rgba(0,0,0,0.04); }
                .dia-item h3 { color: #111111; font-size: 18px; margin-bottom: 8px; }
                .dia-item p { color: #555555; font-size: 15px; line-height: 1.4; }
                .nosotros { background: #eceff1; color: #333333; text-align: center; line-height: 1.8; }
                .nosotros .titulo { color: #111111; }
                .contacto { background: #ffffff; color: #222222; text-align: center; line-height: 2; border-top: 1px solid #e5e5e5; }
                .contacto h2 { color: #111111; margin-bottom: 15px; font-size: 32px; }
                .contacto p { font-size: 16px; margin-bottom: 8px; color: #555555; }
                footer { background: #1a1a1a; color: #aaaaaa; text-align: center; padding: 25px; font-size: 14px; }
                .btn-admin { display: inline-block; margin-top: 10px; background: #333; color: #fff; padding: 6px 14px; border-radius: 4px; text-decoration: none; font-size: 12px; transition: background 0.2s; }
                .btn-admin:hover { background: #ff4d4d; }
                @media (max-width: 768px) {
                    header { justify-content: center; text-align: center; }
                    nav a { display: inline-block; margin: 8px; }
                    .hero { flex-direction: column-reverse; text-align: center; padding: 40px 6%; }
                    .seccion { padding: 40px 6%; }
                }
            </style>
        </head>
        <body>
            <header>
                <a href="#inicio" class="logo">Son <span>D'licias</span> Vivi</a>
                <nav>
                    <a href="#inicio">Inicio</a>
                    <a href="#menu">Menú</a>
                    <a href="#menu-diario">Menú Diario</a>
                    <a href="#nosotros">Nosotros</a>
                    <a href="#contacto">Contacto</a>
                </nav>
            </header>

            <section class="hero" id="inicio">
                <div class="hero-content">
                    <h1>El Mejor Sabor Valluno y Más</h1>
                    <p>Disfruta de platos deliciosos, ingredientes frescos y momentos especiales con las personas que más quieres.</p>
                    <a href="#menu" class="boton">Explorar nuestro menú</a>
                </div>
                <div class="hero-image">
                    <img src="https://i.postimg.cc/xCx3rq5P/Whats-App-Image-2026-10-14-at-9-06-59-PM.jpg" alt="Logo Gigante">
                </div>
            </section>

            <section class="seccion" id="menu">
                <h2 class="titulo">Nuestro menú</h2>
                <p class="subtitulo">Preparado con amor para satisfacer tus antojos.</p>
                <div class="categorias">
                    <a class="categoria" href="#comidas">Hamburguesas</a>
                    <a class="categoria" href="#bebidas">Bebidas</a>
                    <a class="categoria" href="#postres">Postres</a>
                </div>
                <h2 id="comidas" style="margin-bottom:20px; color: #111111;">Almuerzos Bebidas y Postres</h2>
                <div class="productos">
                    <article class="producto">
                        <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRxNj-crP43UedV9odR1QJCcwmdSAmoH4tK7zDkwh3WRpA2vcekIrywi6k2&s=10" alt="Carne de Res asada">
                        <div class="detalle">
                            <h3>Carne de Res asada</h3>
                            <p>Sabor fuerte y ahumado.</p>
                            <div class="precio">$8.000</div>
                            <a class="boton pedido" data-producto="Carne de Res asada" href="#">Pedir ahora</a>
                        </div>
                    </article>
                    <article class="producto">
                        <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSBNP4burjd6nu3qVbKxPXzEkSCW3fFAPKXoNOEhW50eOG4qTubMqjta70p&s=10" alt="Pizza artesanal">
                        <div class="detalle">
                            <h3>Carne de Cerdo asada</h3>
                            <p>Muy tierna y suave. Su grasa natural le da mucha jugosidad y un toque doradito y crujiente por fuera.</p>
                            <div class="precio">$7.000</div>
                            <a class="boton pedido" data-producto="Pizza artesanal" href="#">Pedir ahora</a>
                        </div>
                    </article>
                    <article class="producto">
                        <img src="https://cdn.avena.io/avena-recipes-v2/2019/10/1571781831081.jpeg" alt="Ensalada fresca">
                        <div class="detalle">
                            <h3>Pechuga a la Plancha</h3>
                            <p>Limpia, magra y ligera. Bien marinada y jugosa.</p>
                            <div class="precio">$7.000</div>
                            <a class="boton pedido" data-producto="Pechuga a la Plancha" href="#">Pedir ahora</a>
                        </div>
                    </article>
                </div>
                
                <h2 id="bebidas" style="margin:40px 0 20px; color: #111111;">Bebidas</h2>
                <div class="productos">
                    <article class="producto">
                        <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQIAtVAB4f_b6WX0KDobTqPihwQvNzCPlJ46OHeop8fwhC1kO0mKzchtik9&s=10" alt="Agua de panela">
                        <div class="detalle">
                            <h3>Agua de panela</h3>
                            <p>Bebida refrescante preparada al momento.</p>
                            <div class="precio">$1.500</div>
                            <a class="boton pedido" data-producto="Agua de panela" href="#">Pedir ahora</a>
                        </div>
                    </article>
                    <article class="producto">
                        <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRjLynfbvyAz1-MGF8fdz_eEsgI_PrdMBqbSowV0748tSj-xdPU-yt4aEpb&s=10" alt="Gaseosas">
                        <div class="detalle">
                            <h3>Gaseosas</h3>
                            <div class="precio">$2.500</div>
                            <a class="boton pedido" data-producto="Gaseosas" href="#">Pedir ahora</a>
                        </div>
                    </article>
                </div>

                <h2 id="postres" style="margin:40px 0 20px; color: #111111;">Postres</h2>
                <div class="productos">
                    <article class="producto">
                        <img src="https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=700&q=80" alt="Postre de la casa">
                        <div class="detalle">
                            <h3>Postre de la casa</h3>
                            <p>Un dulce especial para terminar tu comida con una sonrisa.</p>
                            <div class="precio">$10.000</div>
                            <a class="boton pedido" data-producto="Postre de la casa" href="#">Pedir ahora</a>
                        </div>
                    </article>
                </div>
            </section>

            <section class="seccion" id="menu-diario" style="background: #ffffff; border-top: 1px solid #e5e5e5; border-bottom: 1px solid #e5e5e5;">
                <h2 class="titulo">Menú del Día</h2>
                <p class="subtitulo">Descubre la programación de nuestros platos especiales para cada día de la semana.</p>
                <div class="menu-diario-grid">
                    """ + htmlMenuDiario.toString() + """
                </div>
            </section>

            <section class="seccion nosotros" id="nosotros">
                <h2 class="titulo">Sobre Nosotros</h2>
                <div class="mision-vision">
                    <div class="bloque-mv">
                        <h3>Misión</h3>
                        <p>Deleitar a familias, trabajadores y visitantes con el auténtico sabor de la comida casera valluna, elaborada con ingredientes frescos y un servicio cercano y acogedor que haga sentir a cada cliente como en casa.</p>
                    </div>
                    <div class="bloque-mv">
                        <h3>Visión</h3>
                        <p>Ser el restaurante local preferido por nuestra sazón tradicional y calidad humana, consolidándonos como un referente de la gastronomía casera gracias a la frescura, la tradición y la satisfacción de quienes nos visitan.</p>
                    </div>
                </div>
            </section>

            <section class="seccion contacto" id="contacto">
                <h2>¡Queremos atenderte!</h2>
                <p> Dirección: Calle 16 #4-39, Barrio El Jardin</p>
                <p> WhatsApp: 3113879529</p>
                <p> Horario: lunes a domingo, 11:30 a. m. - 03:00 p. m.</p>
                <br>
                <a class="boton" href="https://wa.me/573113879529" target="_blank" style="background-color: #25D366; margin-right: 10px; margin-bottom: 10px; color: white;">WhatsApp</a>
                <a class="boton" href="https://www.facebook.com/share/19W2fPbrMY/?mibextid=wwXIfr" target="_blank" style="background-color: #1877f2; margin-right: 10px; margin-bottom: 10px; color: white;">Facebook</a>
                <a class="boton" href="https://www.instagram.com/son_deliciasvivi?dlrf=eTFrYXM1YXNhb3Rn&utm_source=qr" target="_blank" style="background-color: #e1306c; margin-bottom: 10px; color: white;">Instagram</a>
            </section>

            <footer>
                <p>© 2026 Son Delicias Vivi.</p>
                <a href="/admin" class="btn-admin">⚙️ Acceso Dueño (Editar Menú)</a>
            </footer>

            <script>
                const telefono = "573113879529"; 
                document.querySelectorAll(".pedido").forEach(
                    boton => {
                        const producto = boton.dataset.producto;
                        const mensaje = "Hola, quiero pedir: " + producto;
                        boton.href = "https://wa.me/" + telefono + "?text=" + encodeURIComponent(mensaje);
                        boton.target = "_blank";
                    }
                );
            </script>
        </body>
        </html>
        """;
    }

    // ==========================================
    // PÁGINA DE LOGIN
    // ==========================================
    public static String crearPaginaLogin() {
        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Login Dueño | Son Delicias Vivi</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: Arial, sans-serif; background: #f4f6f8; display: flex; justify-content: center; align-items: center; height: 100vh; }
                .login-box { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); width: 100%; max-width: 380px; border: 1px solid #ddd; text-align: center; }
                h2 { margin-bottom: 20px; color: #111; font-size: 22px; }
                input { width: 100%; padding: 12px; margin-bottom: 15px; border: 1px solid #ccc; border-radius: 6px; font-size: 15px; }
                .boton { background: #ff4d4d; color: white; padding: 12px; border: none; border-radius: 8px; font-weight: bold; width: 100%; cursor: pointer; font-size: 15px; transition: background 0.2s; }
                .boton:hover { background: #e63946; }
                .volver { display: block; margin-top: 15px; color: #666; text-decoration: none; font-size: 13px; }
                .volver:hover { color: #ff4d4d; text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="login-box">
                <h2>Panel de Dueño</h2>
                <form action="/admin/login" method="POST">
                    <input type="password" name="password" placeholder="Contraseña de Administrador" required autofocus>
                    <button type="submit" class="boton">Ingresar</button>
                </form>
                <a href="/" class="volver">← Volver al sitio web</a>
            </div>
        </body>
        </html>
        """;
    }

    // ==========================================
    // PANEL DE ADMINISTRACIÓN
    // ==========================================
    public static String crearPaginaAdmin() {
        StringBuilder listaAdmin = new StringBuilder();

        for (Map.Entry<String, String> entry : menuDiario.entrySet()) {
            listaAdmin.append("""
                <div class="admin-item-row">
                    <div><strong>%s:</strong> %s</div>
                    <form action="/admin/eliminar" method="POST" style="margin:0;">
                        <input type="hidden" name="dia" value="%s">
                        <button type="submit" class="btn-eliminar">Eliminar</button>
                    </form>
                </div>
            """.formatted(entry.getKey(), entry.getValue(), entry.getKey()));
        }

        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Gestionar Menú | Son Delicias Vivi</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: Arial, sans-serif; background: #f4f6f8; color: #333333; padding: 40px 20px; }
                .container { max-width: 650px; margin: 0 auto; background: #ffffff; padding: 35px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); border: 1px solid #ddd; }
                h1 { text-align: center; color: #111; margin-bottom: 8px; font-size: 26px; }
                .sub { text-align: center; color: #666; margin-bottom: 25px; font-size: 14px; }
                label { display: block; margin-bottom: 6px; font-weight: bold; color: #333; margin-top: 15px; }
                input { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ccc; border-radius: 6px; font-size: 15px; }
                .boton { display: inline-block; background: #ff4d4d; color: white; padding: 12px; text-decoration: none; border-radius: 8px; font-weight: bold; border: none; cursor: pointer; width: 100%; text-align: center; transition: background 0.2s; }
                .boton:hover { background: #e63946; }
                .admin-item-row { display: flex; justify-content: space-between; align-items: center; background: #fafafa; padding: 12px; margin-bottom: 10px; border: 1px solid #eee; border-radius: 6px; gap: 10px; }
                .btn-eliminar { background: #dc3545; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-weight: bold; font-size: 12px; }
                .btn-eliminar:hover { background: #c82333; }
                .volver { display: block; text-align: center; margin-top: 25px; color: #555; text-decoration: none; font-size: 14px; font-weight: bold; }
                .volver:hover { color: #ff4d4d; text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Panel de Dueño</h1>
                <p class="sub">Escribe el día o fecha, el plato y publica los cambios al instante</p>

                <!-- Formulario para Agregar o Modificar con cuadro de texto libre -->
                <form action="/admin/guardar" method="POST">
                    <h3 style="font-size: 16px; color: #111; border-bottom: 1px solid #eee; padding-bottom: 8px;">Agregar o Modificar Menú Diario</h3>
                    
                    <label for="dia">Día o Fecha (ej: Domingo 18 de Octubre, Lunes):</label>
                    <input type="text" name="dia" placeholder="Escribe el día o la fecha exacta" required>

                    <label for="plato">Plato / Descripción (ej: Para hoy tendremos...):</label>
                    <input type="text" name="plato" placeholder="Escribe el menú o descripción" required>

                    <button type="submit" class="boton" style="margin-top: 10px;">Guardar y Publicar</button>
                </form>

                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">

                <!-- Listado para Eliminar Directo -->
                <h3 style="font-size: 16px; color: #111; margin-bottom: 15px;">Menús Activos (Eliminar)</h3>
                <div style="max-height: 300px; overflow-y: auto;">
                    """ + listaAdmin.toString() + """
                </div>

                <a href="/" class="volver">← Ver la página principal pública</a>
            </div>
        </body>
        </html>
        """;
    }
}
