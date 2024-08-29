import { Bransje, ForerkortKlasse, InnholdElement, Sertifisering } from "@mr/api-client";
import z from "zod";

const InnholdElementerSchema = z
  .nativeEnum(InnholdElement, { required_error: "Du må velge minst ett element" })
  .array()
  .nonempty("Du må velge minst ett element");

export const AmoKategoriseringSchema = z.discriminatedUnion("kurstype", [
  z.object({
    kurstype: z.literal("BRANSJE_OG_YRKESRETTET"),
    bransje: z.nativeEnum(Bransje, { required_error: "Du må velge bransje" }),
    sertifiseringer: z.custom<Sertifisering>().array().default([]),
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
