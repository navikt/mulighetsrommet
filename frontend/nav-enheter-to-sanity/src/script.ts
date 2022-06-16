export function run(id: string, app: (id: string) => Promise<void>) {
  console.info(`[${id}] Starting...`);

  app(id)
    .then(() => {
      console.info(`[${id}] Done!`);
      process.exit(0);
    })
    .catch((error) => {
      console.error(`[${id}]`, error ?? "Failed!");
      process.exit(1);
    });
}
