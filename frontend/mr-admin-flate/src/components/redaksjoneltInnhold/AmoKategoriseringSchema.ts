import { Bransje, ForerkortKlasse, InnholdElement, Sertifisering } from "@mr/api-client";
import z from "zod";

const InnholdElementerSchema = z
  .nativeEnum(InnholdElement, { errorMap: () => ({ message: "Du må velge minst ett element" }) })
  .array()
  .nonempty("Du må velge minst ett element");

export const AmoKategoriseringSchema = z.discriminatedUnion("kurstype", [
  z.object({
    kurstype: z.literal("BRANSJE_OG_YRKESRETTET"),
    bransje: z.nativeEnum(Bransje, { errorMap: () => ({ message: "Du må velge bransje" }) }),
    sertifiseringer: z
      .custom<Sertifisering>()
      .array()
      .nullish()
      .transform((val) => {
        if (!val) {
          return [];
        }
        return val;
      })
      .pipe(z.custom<Sertifisering>().array()),
    forerkort: z.nativeEnum(ForerkortKlasse).array().default([]),
    innholdElementer: InnholdElementerSchema,
  }),
  z.object({
    kurstype: z.literal("NORSKOPPLAERING"),
    norskprove: z.boolean().default(false),
    innholdElementer: InnholdElementerSchema,
  }),
  z.object({
    kurstype: z.literal("GRUNNLEGGENDE_FERDIGHETER"),
    innholdElementer: InnholdElementerSchema,
  }),
  z.object({
    kurstype: z.literal("FORBEREDENDE_OPPLAERING_FOR_VOKSNE"),
  }),
  z.object({
    kurstype: z.literal("STUDIESPESIALISERING"),
  }),
]);
