/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  ArnesBatalla.java (prueba de consola, no va en el APK)
 */

import com.juego.combates.batalla.Batalla;
import com.juego.combates.batalla.Evento;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Fabrica;
import com.juego.combates.modelo.Movimiento;
import com.juego.combates.modelo.Tipo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Arnés de pruebas del motor del juego (paquetes modelo y batalla).
 *
 * No necesita Android: se compila con el javac del PC junto a las clases de
 * modelo/ y batalla/. Vuelca por consola la tabla de tipos, las fichas de las
 * seis especies y después juega 300 combates completos con semillas fijas, de
 * forma que el resultado es siempre el mismo. La salida se compara con el
 * archivo pruebas/referencia_motor.txt: si tras un cambio hay alguna línea
 * distinta es que el motor ha cambiado de comportamiento (textos, fórmulas u
 * orden de las llamadas a Random). Los comandos están en pruebas/README.md.
 */
public class ArnesBatalla {

    /** Cuántos combates se juegan. Con 300 salen todas las combinaciones de equipos habituales. */
    private static final int NUMERO_COMBATES = 300;

    /** Tope de turnos por combate para que un fallo del motor no deje el arnés colgado. */
    private static final int MAXIMO_TURNOS = 500;

    /**
     * Multiplico la semilla del Random de "selección" por este primo para que
     * no vaya en paralelo con la del motor (que es la semilla tal cual).
     */
    private static final long MULTIPLICADOR_SEMILLA = 7919L;
    /** Tanto por ciento de turnos en que el jugador cambia de criatura a propósito. */
    private static final int PORCENTAJE_CAMBIO = 15;
    /**
     * nextInt(MOVIMIENTOS_MAS_UNO) da de 0 a 4: uno más que movimientos hay,
     * para que a veces salga un índice fuera de rango (y con el "- 1" de abajo, también -1).
     */
    private static final int MOVIMIENTOS_MAS_UNO = 5;

    /**
     * Imprime un evento del combate en una sola línea con todo lo que puede
     * variar: texto, efecto y la vida de las dos criaturas en ese instante.
     *
     * @param e evento generado por el motor
     */
    private static void imprimirEvento(Evento e) {
        System.out.println(e.getTexto() + " | " + e.getEfecto() + " | "
                + e.getJugador().getNombre() + " " + e.getHpJugador() + " | "
                + e.getRival().getNombre() + " " + e.getHpRival());
    }

    /**
     * Imprime una lista de eventos en orden.
     *
     * @param lista eventos devueltos por una acción del motor
     */
    private static void imprimirEventos(List<Evento> lista) {
        for (int i = 0; i < lista.size(); i++) {
            imprimirEvento(lista.get(i));
        }
    }

    /**
     * Busca la primera criatura del equipo que no esté debilitada y no sea la activa.
     *
     * @param equipo equipo del jugador
     * @param activa criatura que está combatiendo ahora
     * @return índice de la candidata, o -1 si no queda ninguna
     */
    private static int buscarSuplente(ArrayList<Criatura> equipo, Criatura activa) {
        int indice = -1;
        for (int i = 0; i < equipo.size(); i++) {
            if (!equipo.get(i).estaDebilitada() && equipo.get(i) != activa) {
                indice = i;
                break;
            }
        }
        return indice;
    }

    /**
     * Punto de entrada. No recibe argumentos: todo es fijo para que la salida sea reproducible.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        // Primero la tabla de efectividades completa: cada tipo contra cada tipo.
        Tipo[] tipos = Tipo.values();
        for (int i = 0; i < tipos.length; i++) {
            for (int j = 0; j < tipos.length; j++) {
                System.out.println("TIPO " + tipos[i] + " " + tipos[i].getNombre() + " " + tipos[i].getColor()
                        + " vs " + tipos[j] + " = " + tipos[i].multiplicadorContra(tipos[j]));
            }
        }

        // Después las fichas de las especies. Incluyo un nombre inexistente para
        // comprobar qué devuelve la fábrica en ese caso.
        String[] especies = {"Fogarto", "Acuarin", "Hojaruz", "Chispin", "Rocardo", "Normalino", "NoExiste"};
        for (int i = 0; i < especies.length; i++) {
            Criatura c = Fabrica.crear(especies[i]);
            System.out.println("CRI " + c.getNombre() + " " + c.getTipo() + " " + c.getNivel() + " " + c.getHpMax()
                    + " " + c.getAtaque() + " " + c.getDefensa() + " " + c.getVelocidad() + " " + c.getHp());
            List<Movimiento> movimientos = c.getMovimientos();
            for (int j = 0; j < movimientos.size(); j++) {
                Movimiento m = movimientos.get(j);
                System.out.println("  MOV " + m.getNombre() + " " + m.getTipo() + " " + m.getPotencia() + " "
                        + m.getPrecision() + " " + m.getPpMax() + " " + m.getPp() + " " + m.esCuracion());
            }
        }
        System.out.println("TAM " + Fabrica.TAMANO_EQUIPO + " " + Arrays.toString(Fabrica.getEspecies()));

        // Y por último los combates. Uso dos Random distintos: "seleccion" decide
        // los equipos y las acciones del jugador (lo que en la app haría la
        // persona) y "rng" es el que recibe el motor. Así cualquier cambio en el
        // orden de llamadas a Random dentro del motor se nota en la salida.
        String[] nombres = {"Fogarto", "Acuarin", "Hojaruz", "Chispin", "Rocardo", "Normalino"};
        for (int semilla = 1; semilla <= NUMERO_COMBATES; semilla++) {
            Random seleccion = new Random(semilla * MULTIPLICADOR_SEMILLA);
            List<String> barajados = new ArrayList<String>(Arrays.asList(nombres));

            Collections.shuffle(barajados, seleccion);
            ArrayList<Criatura> equipoJugador = new ArrayList<Criatura>();
            for (int i = 0; i < Fabrica.TAMANO_EQUIPO; i++) {
                equipoJugador.add(Fabrica.crear(barajados.get(i)));
            }

            Collections.shuffle(barajados, seleccion);
            ArrayList<Criatura> equipoRival = new ArrayList<Criatura>();
            for (int i = 0; i < Fabrica.TAMANO_EQUIPO; i++) {
                equipoRival.add(Fabrica.crear(barajados.get(i)));
            }

            Random rng = new Random(semilla);
            Batalla batalla = new Batalla(equipoJugador, equipoRival, rng);
            System.out.println("=== SEED " + semilla);
            imprimirEventos(batalla.eventosIntro());

            int turnos = 0;
            while (!batalla.rivalDerrotado() && !batalla.jugadorDerrotado() && turnos < MAXIMO_TURNOS) {
                if (batalla.getActivaJugador().estaDebilitada()) {
                    // Mi criatura ha caído: el cambio es obligatorio y gratis.
                    int indice = buscarSuplente(equipoJugador, batalla.getActivaJugador());
                    if (indice < 0) {
                        break;
                    }
                    imprimirEventos(batalla.cambioTrasKO(indice));
                } else {
                    // Un PORCENTAJE_CAMBIO de las veces cambio de criatura a propósito;
                    // el resto ataco. El movimiento sale de nextInt(MOVIMIENTOS_MAS_UNO)
                    // - (r % 2), que a veces da -1 o 4 para probar índices fuera de rango.
                    int r = seleccion.nextInt(100);
                    int indice = buscarSuplente(equipoJugador, batalla.getActivaJugador());
                    if (r < PORCENTAJE_CAMBIO && indice >= 0) {
                        imprimirEventos(batalla.cambioVoluntario(indice));
                    } else {
                        imprimirEventos(batalla.ejecutarTurno(seleccion.nextInt(MOVIMIENTOS_MAS_UNO) - (r % 2)));
                    }
                }
                turnos++;
            }

            System.out.println("FIN rivalDerrotado=" + batalla.rivalDerrotado()
                    + " jugadorDerrotado=" + batalla.jugadorDerrotado() + " turnos=" + turnos);
            for (int i = 0; i < equipoJugador.size(); i++) {
                Criatura c = equipoJugador.get(i);
                System.out.println("  J " + c.getNombre() + " " + c.getHp() + " " + c.tieneAlgunPP() + " " + c.fraccionVida());
            }
            for (int i = 0; i < equipoRival.size(); i++) {
                Criatura c = equipoRival.get(i);
                System.out.println("  R " + c.getNombre() + " " + c.getHp() + " " + c.tieneAlgunPP() + " " + c.fraccionVida());
            }
        }
    }
}
