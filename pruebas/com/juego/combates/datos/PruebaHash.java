/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  PruebaHash.java (prueba de consola, no va en el APK)
 */
package com.juego.combates.datos;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Autocomprobación de Hash.java desde el PC (no en el móvil): compara mi
 * PBKDF2-HMAC-SHA256 con vectores de prueba conocidos y con el del JDK, y
 * comprueba que verificar() acepta la contraseña buena y rechaza la mala.
 *
 * Va en el mismo paquete que Hash (com.juego.combates.datos) para poder llamar
 * a derivar, aHex y deHex, que son package-private a propósito: no son parte de
 * la API de la app, solo los necesitan el repositorio y esta prueba.
 *
 * Compilar y ejecutar desde la raíz del proyecto (ver pruebas/README.md):
 *   javac -encoding UTF-8 -d pruebas/salida app/src/main/java/com/juego/combates/datos/Hash.java pruebas/com/juego/combates/datos/PruebaHash.java
 *   java -cp pruebas/salida com.juego.combates.datos.PruebaHash
 * Termina imprimiendo TODO OK o FALLO. No uso System.exit: la última línea ya
 * dice cómo ha ido y así el programa se puede lanzar desde cualquier sitio.
 */
public class PruebaHash {

    /**
     * Lanza todas las comprobaciones y resume al final.
     *
     * @param args no se usan
     * @throws Exception si el JDK no tiene PBKDF2WithHmacSHA256 (en un JDK 17+ siempre lo tiene)
     */
    public static void main(String[] args) throws Exception {
        // cada comprobación va delante del && para que se ejecute (y se imprima)
        // aunque una anterior haya fallado; todoBien solo acumula el resultado
        boolean todoBien = true;

        // Vectores clásicos de PBKDF2-HMAC-SHA256 (password="password", salt="salt")
        todoBien = comprobarVector("RFC (c=1, 32 bytes)",
                "password", "salt", 1, 32,
                "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b") && todoBien;
        todoBien = comprobarVector("RFC (c=2, 32 bytes)",
                "password", "salt", 2, 32,
                "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43") && todoBien;
        todoBien = comprobarVector("RFC (c=4096, 32 bytes)",
                "password", "salt", 4096, 32,
                "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a") && todoBien;
        // Vectores del RFC 7914 (sección 11), con dos bloques de salida
        todoBien = comprobarVector("RFC 7914 (passwd/salt, c=1, 64 bytes)",
                "passwd", "salt", 1, 64,
                "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
                        + "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783") && todoBien;
        todoBien = comprobarVector("RFC 7914 (Password/NaCl, c=80000, 64 bytes)",
                "Password", "NaCl", 80000, 64,
                "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56"
                        + "a1d425a1225833549adb841b51c9b3176a272bdebba1d078478f62b397f33c8d") && todoBien;

        // Comparación con el PBKDF2 del JDK de escritorio con mis parámetros reales
        byte[] salt = new byte[Hash.TAM_SALT];
        new SecureRandom().nextBytes(salt);
        String clave = "Contraseña-de-prueba-ñ-2026";
        SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), salt, Hash.ITERACIONES, Hash.TAM_HASH * 8);
        byte[] delJdk = fabrica.generateSecret(spec).getEncoded();
        byte[] mio = Hash.derivar(clave.getBytes(StandardCharsets.UTF_8), salt, Hash.ITERACIONES, Hash.TAM_HASH);
        todoBien = informar("SecretKeyFactory del JDK (120000 it., salt aleatorio)", Arrays.equals(delJdk, mio)) && todoBien;

        // Ida y vuelta con el formato completo, midiendo lo que tarda
        long inicio = System.nanoTime();
        String guardado = Hash.hashear("MiClave123");
        long ms = (System.nanoTime() - inicio) / 1000000L;
        System.out.println("  hash de ejemplo: " + guardado + "  (" + ms + " ms)");
        todoBien = informar("verificar(correcta)", Hash.verificar("MiClave123", guardado)) && todoBien;
        todoBien = informar("verificar(incorrecta) = false", !Hash.verificar("MiClave124", guardado)) && todoBien;
        todoBien = informar("verificar(cadena rota) = false", !Hash.verificar("MiClave123", "basura$sin$formato")) && todoBien;
        todoBien = informar("dos hashes de la misma clave son distintos (salt)",
                !guardado.equals(Hash.hashear("MiClave123"))) && todoBien;

        // Hexadecimal: ida y vuelta y rechazo de cadenas que no son hex
        byte[] bytesHex = Hash.deHex(Hash.aHex(salt));
        todoBien = informar("deHex(aHex(x)) = x", Arrays.equals(salt, bytesHex)) && todoBien;
        todoBien = informar("deHex('zz') = null", Hash.deHex("zz") == null) && todoBien;

        // Normalización de respuestas
        todoBien = informar("normalizar('  Tobías   PÉREZ ') = 'tobias perez'",
                "tobias perez".equals(Hash.normalizarRespuesta("  Tobías   PÉREZ "))) && todoBien;
        todoBien = informar("normalizar(null) = ''", "".equals(Hash.normalizarRespuesta(null))) && todoBien;

        // hash "ficticio" para el login de usuarios que no existen (se pega en RepositorioUsuarios)
        System.out.println("  hash ficticio: " + Hash.hashear("monster-clash-cuenta-que-no-existe"));

        if (todoBien) {
            System.out.println("TODO OK");
        } else {
            System.out.println("FALLO");
        }
    }

    /**
     * Deriva una clave con mi PBKDF2 y la compara con el valor esperado del vector.
     *
     * @param nombre      etiqueta que sale en pantalla
     * @param password    contraseña del vector
     * @param salt        salt del vector
     * @param iteraciones vueltas de PBKDF2
     * @param longitud    bytes de salida
     * @param esperadoHex resultado publicado, en hexadecimal minúscula
     * @return true si coincide
     */
    private static boolean comprobarVector(String nombre, String password, String salt,
                                           int iteraciones, int longitud, String esperadoHex) {
        byte[] obtenido = Hash.derivar(password.getBytes(StandardCharsets.UTF_8),
                salt.getBytes(StandardCharsets.UTF_8), iteraciones, longitud);
        return informar(nombre, esperadoHex.equals(Hash.aHex(obtenido)));
    }

    /**
     * Imprime una línea [OK] o [FALLO] y devuelve el resultado para ir acumulándolo.
     *
     * @param nombre etiqueta de la comprobación
     * @param ok     si ha pasado
     * @return el mismo ok
     */
    private static boolean informar(String nombre, boolean ok) {
        if (ok) {
            System.out.println("[OK]    " + nombre);
        } else {
            System.out.println("[FALLO] " + nombre);
        }
        return ok;
    }
}
