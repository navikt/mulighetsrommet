import { createClient } from "@sanity/client";
import { EnhetskontaktinfoService, OpenAPI } from "norg2-api-client";
import {
  spesialEnheterToSanity,
  fylkeOgUnderenheterToSanity,
} from "./sanity-enhet";
import { run } from "./script";

OpenAPI.BASE = process.env.NORG2_API_ENDPOINT ?? "";

const client = createClient({
  projectId: process.env.SANITY_PROJECT_ID,
  dataset: process.env.SANITY_DATASET,
  token: process.env.SANITY_AUTH_TOKEN,
  apiVersion: "2021-03-25",
  useCdn: true,
});

run("NORG2 -> Sanity", app);

async function app(id: string) {
  console.info("Henter enheter fra NORG2...");
  const enheter =
    await EnhetskontaktinfoService.hentAlleEnheterInkludertKontaktinformasjonUsingGet(
      { consumerId: id }
    );

  console.info("Konverterer enheter...");
  const spesialEnheter = spesialEnheterToSanity(enheter, ["ALS"]);
  const fylkeOgUnderenheter = fylkeOgUnderenheterToSanity(enheter);
  const enheterSomSkalSynces = [...fylkeOgUnderenheter, ...spesialEnheter];

  console.info("Skriver enheter til Sanity...");
  const transaction = client.transaction();
  for (const enhet of enheterSomSkalSynces) {
    transaction.createOrReplace(enhet);
  }
  const response = await transaction.commit();
  console.info("Oppdaterte enheter:", response.documentIds.length);

  //TODO: Attempt to delete enheter that has status  "Nedlagt" / "Under avvikling" and error if they are referenced by other documents?
}
