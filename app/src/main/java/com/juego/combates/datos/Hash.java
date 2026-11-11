/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Hash.java
 */
package com.juego.combates.datos;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilidad para guardar contraseñas (y respuestas de seguridad) de forma segura.
 *
 * Nunca guardo la contraseña tal cual: guardo un hash PBKDF2-HMAC-SHA256 con un
 * salt aleatorio de 16 bytes y 120000 iteraciones. El salt hace que dos usuarios
 * con la misma contraseña tengan hashes distintos, y las iteraciones hacen que
 * probar contraseñas a lo bruto sea lento a propósito (unos 100-400 ms por
 * intento en un móvil).
 *
 * El PBKDF2 lo implemento A MANO encima de javax.crypto.Mac("HmacSHA256"), que
 * sí está garantizado en todos los Android. Lo hago así porque en API 24 (mi
 * minSdk) el SDK no garantiza que SecretKeyFactory tenga el algoritmo
 * "PBKDF2WithHmacSHA256" (llegó de forma fiable en API 26), y no quiero que la
 * app pete en un móvil con Android 7. La implementación sigue el RFC 8018
 * (sección 5.2): T_i = U_1 xor U_2 xor ... xor U_c, con U_1 = HMAC(P, S || i).
 *
 * Esta clase NO importa nada de android.* a propósito: así la puedo compilar
 * y probar con el javac normal del PC. La prueba está fuera del APK, en
 * pruebas/com/juego/combates/datos/PruebaHash.java, y compara mi PBKDF2 con el
 * del JDK y con los vectores de prueba oficiales.
 *
 * derivar, aHex y deHex son package-private (sin public ni private) a
 * propósito: no forman parte de la API de la app (eso son hashear, verificar
 * y normalizarRespuesta), pero los necesitan RepositorioUsuarios,
 * TokenRecuperacion y la prueba, que están en este mismo paquete. Así no los
 * ve nadie más.
 *
 * Formato que guardo en la base de datos, todo en una sola cadena de texto:
 *   pbkdf2_sha256$120000$&lt;salt en hexadecimal&gt;$&lt;hash en hexadecimal&gt;
 * Así, si algún día subo las iteraciones, los hashes viejos siguen valiendo
 * porque cada uno lleva las suyas. Uso hexadecimal propio en vez de Base64
 * porque java.util.Base64 es de API 26 y android.util.Base64 metería android.*.
 */
public final class Hash {

    /** Nombre del algoritmo que va delante en la cadena guardada. */
    public static final String ALGORITMO = "pbkdf2_sha256";
    /** Iteraciones de PBKDF2. Cuantas más, más lento para un atacante (y para mí). */
    public static final int ITERACIONES = 120000;
    /** Límite defensivo para que un registro manipulado no provoque un DoS local. */
    private static final int ITERACIONES_MAXIMAS = 1000000;
    /** Tamaño del salt en bytes. */
    public static final int TAM_SALT = 16;
    /** Tamaño del hash de salida en bytes (32 = lo que da SHA-256). */
    public static final int TAM_HASH = 32;

    private static final String NOMBRE_HMAC = "HmacSHA256";
    private static final char[] DIGITOS_HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom ALEATORIO = new SecureRandom();

    // clase de utilidad: no se instancia
    private Hash() {
    }

    // ---- API pública ----

    /**
     * Genera un salt nuevo y devuelve la cadena completa lista para guardar.
     *
     * @param secreto contraseña o respuesta en claro
     * @return cadena "pbkdf2_sha256$iteraciones$saltHex$hashHex"
     */
    public static String hashear(String secreto) {
        return hashear(secreto.toCharArray());
    }

    /**
     * Igual que hashear(String) pero con char[], que es lo que devuelven los
     * campos de contraseña y que puedo borrar de memoria después.
     *
     * @param secreto contraseña o respuesta en claro
     * @return cadena "pbkdf2_sha256$iteraciones$saltHex$hashHex"
     */
    public static String hashear(char[] secreto) {
        byte[] salt = new byte[TAM_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] clave = derivar(aBytes(secreto), salt, ITERACIONES, TAM_HASH);
        return ALGORITMO + "$" + ITERACIONES + "$" + aHex(salt) + "$" + aHex(clave);
    }

    /**
     * Comprueba si un secreto se corresponde con una cadena guardada.
     *
     * Saco el salt y las iteraciones de la propia cadena, vuelvo a calcular el
     * hash y comparo con MessageDigest.isEqual, que tarda lo mismo acierte o
     * no (si comparara byte a byte y parara en el primero distinto, midiendo
     * tiempos se podría ir adivinando el hash).
     *
     * @param secreto    lo que ha escrito el usuario
     * @param almacenado la cadena que hay en la base de datos
     * @return true si coincide; false si no, o si la cadena está mal formada
     */
    public static boolean verificar(String secreto, String almacenado) {
        if (secreto == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !ALGORITMO.equals(partes[0])) {
            return false;
        }
        int iteraciones;
        try {
            iteraciones = Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] salt = deHex(partes[2]);
        byte[] esperado = deHex(partes[3]);
        if (iteraciones < 1 || iteraciones > ITERACIONES_MAXIMAS
                || salt == null || salt.length != TAM_SALT
                || esperado == null || esperado.length != TAM_HASH) {
            return false;
        }
        byte[] calculado = derivar(aBytes(secreto.toCharArray()), salt, iteraciones, esperado.length);
        return MessageDigest.isEqual(calculado, esperado);
    }

    /**
     * Deja la respuesta de seguridad en una forma "canónica" antes de hashearla,
     * para que "  Tobías " y "tobias" cuenten como la misma respuesta:
     * quito espacios de los lados, paso a minúsculas, junto los espacios
     * seguidos en uno y quito las tildes (con Normalizer descompongo "á" en
     * "a" + acento y borro el acento; la "ñ" también se queda en "n").
     *
     * @param respuesta lo que ha escrito el usuario (puede ser null)
     * @return la respuesta normalizada ("" si venía null)
     */
    public static String normalizarRespuesta(String respuesta) {
        if (respuesta == null) {
            return "";
        }
        String r = Normalizer.normalize(respuesta, Normalizer.Form.NFD);
        r = r.replaceAll("\\p{M}", "");           // fuera acentos y diéresis
        r = r.toLowerCase(Locale.ROOT);
        r = r.replaceAll("\\s+", " ");            // varios espacios -> uno
        return r.trim();
    }

    // ---- PBKDF2 a mano ----

    /**
     * PBKDF2 según el RFC 8018 con HMAC-SHA256 como función base.
     *
     * Para cada bloque i: U_1 = HMAC(clave=password, salt || i en 4 bytes
     * big-endian), U_j = HMAC(password, U_(j-1)) y el bloque es el XOR de
     * todas las U. Con 32 bytes de salida solo hace falta un bloque, pero lo
     * dejo general para poder comprobarlo con los vectores de 64 bytes.
     *
     * @param password contraseña en bytes UTF-8 (se borra al terminar)
     * @param salt     salt aleatorio
     * @param iteraciones número de vueltas (c en el RFC)
     * @param longitud bytes de salida que quiero (dkLen en el RFC)
     * @return la clave derivada
     */
    static byte[] derivar(byte[] password, byte[] salt, int iteraciones, int longitud) {
        if (password.length == 0) {
            // SecretKeySpec no admite claves vacías; en la app nunca pasa porque
            // valido antes las longitudes, pero prefiero un error claro
            throw new IllegalArgumentException("El secreto no puede estar vacío");
        }
        Mac mac;
        try {
            mac = Mac.getInstance(NOMBRE_HMAC);
            mac.init(new SecretKeySpec(password, NOMBRE_HMAC));
        } catch (NoSuchAlgorithmException e) {
            // HmacSHA256 está en todos los Android y JDK, así que no debería pasar
            throw new IllegalStateException("No hay HmacSHA256 en este dispositivo", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Clave HMAC no válida", e);
        } finally {
            Arrays.fill(password, (byte) 0); // ya está dentro del Mac, la borro de aquí
        }

        int tamBloque = mac.getMacLength(); // 32 con SHA-256
        int numBloques = (longitud + tamBloque - 1) / tamBloque;
        byte[] salida = new byte[longitud];
        int pos = 0;

        for (int i = 1; i <= numBloques; i++) {
            // U_1 = HMAC(P, S || INT(i))
            mac.update(salt);
            mac.update((byte) (i >>> 24));
            mac.update((byte) (i >>> 16));
            mac.update((byte) (i >>> 8));
            mac.update((byte) i);
            byte[] u = mac.doFinal();
            byte[] t = u.clone(); // T_i empieza valiendo U_1

            // U_j = HMAC(P, U_(j-1)) y voy acumulando el XOR en t
            for (int j = 1; j < iteraciones; j++) {
                u = mac.doFinal(u);
                for (int k = 0; k < t.length; k++) {
                    t[k] ^= u[k];
                }
            }

            int copiar = Math.min(tamBloque, longitud - pos);
            System.arraycopy(t, 0, salida, pos, copiar);
            pos += copiar;
        }
        return salida;
    }

    // ---- Ayudantes ----

    /**
     * Pasa los caracteres a bytes UTF-8 sin pasar por un String intermedio
     * (los String no se pueden borrar de memoria; los arrays sí).
     *
     * @param caracteres la contraseña o respuesta tal cual
     * @return sus bytes en UTF-8
     */
    private static byte[] aBytes(char[] caracteres) {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(caracteres));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * @param datos bytes a codificar
     * @return los bytes en hexadecimal minúscula (2 caracteres por byte)
     */
    static String aHex(byte[] datos) {
        char[] salida = new char[datos.length * 2];
        for (int i = 0; i < datos.length; i++) {
            int v = datos[i] & 0xFF;
            salida[i * 2] = DIGITOS_HEX[v >>> 4];
            salida[i * 2 + 1] = DIGITOS_HEX[v & 0x0F];
        }
        return new String(salida);
    }

    /**
     * @param hex cadena hexadecimal
     * @return los bytes, o null si la cadena no es hexadecimal válida
     */
    static byte[] deHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        byte[] salida = new byte[hex.length() / 2];
        for (int i = 0; i < salida.length; i++) {
            int alto = Character.digit(hex.charAt(i * 2), 16);
            int bajo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (alto < 0 || bajo < 0) {
                return null;
            }
            salida[i] = (byte) ((alto << 4) | bajo);
        }
        return salida;
    }
}
