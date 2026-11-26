/**
 * Ejecuta `task` una vez por item, arrancándolos en el orden recibido y sin tener nunca más de `limit` a la vez.
 *
 * Un fallo de una tarea no detiene a las demás: cada tarea debe anotar por sí misma su resultado (qué significa un
 * fallo lo decide quien llama), así que el rechazo no se propaga. Cuando `signal` se aborta no arranca ninguna
 * tarea más; las que ya estaban en curso no se interrumpen desde aquí (deben atender esa misma señal).
 *
 * Termina cuando todas las tareas arrancadas han acabado.
 */
export async function runWithConcurrencyLimit<T>(
  items: readonly T[],
  limit: number,
  task: (item: T) => Promise<unknown>,
  signal?: AbortSignal,
): Promise<void> {
  // Índice del siguiente item sin repartir, compartido por todos los workers.
  let nextIndex = 0;

  // Un worker es un bucle que va cogiendo items de uno en uno. El tope sale de cuántos workers hay vivos, no de
  // contar tareas en curso.
  async function runWorker(): Promise<void> {
    while (nextIndex < items.length && !signal?.aborted) {
      // Sin ningún `await` entre la comprobación del `while` y este incremento, ningún otro worker puede ejecutarse
      // en medio (JavaScript es monohilo y solo cede el control en un `await`), así que dos workers nunca cogen el
      // mismo item.
      const item = items[nextIndex++];
      try {
        await task(item);
      } catch {
        // La tarea ya informa de su propio fallo; que un item falle no debe impedir que se procesen los demás.
      }
    }
  }

  const workerCount = Math.min(limit, items.length);
  await Promise.all(Array.from({ length: workerCount }, runWorker));
}
