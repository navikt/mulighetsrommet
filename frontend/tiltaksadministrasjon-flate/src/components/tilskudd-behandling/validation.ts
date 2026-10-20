import {
  OpplaeringtilskuddKode,
  TilskuddMottaker,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import z from "zod";

export const saksopplysningerSchema = z
  .object({
    tilskudd: z
      .array(
        z
          .object({
            soknadJournalpostId: z
              .string({ error: "Journalpost-ID er påkrevd" })
              .min(1, "Journalpost-ID er påkrevd"),
            soknadDato: z
              .string({ error: "Søknadsdato er påkrevd" })
              .min(1, "Søknadsdato er påkrevd"),
            periodeStart: z
              .string({ error: "Periodestart er påkrevd" })
              .min(1, "Periodestart er påkrevd"),
            periodeSlutt: z
              .string({ error: "Periodeslutt er påkrevd" })
              .min(1, "Periodeslutt er påkrevd"),
            kostnadssted: z
              .string({ error: "Kostnadssted er påkrevd" })
              .min(1, "Kostnadssted er påkrevd"),
            tilskuddOpplaeringType: z.enum(OpplaeringtilskuddKode, {
              error: "Tilskuddstype er påkrevd",
            }),
            soknadBelop: z
              .object({
                belop: z
                  .number({ error: "Beløp må være et heltall" })
                  .int("Beløp må være et heltall"),
              })
              .loose(),
            utbetalingMottaker: z.enum(TilskuddMottaker, {
              error: "Mottaker må være satt",
            }),
          })
          .loose(),
      )
      .min(1, "Minst ett tilskudd er påkrevd"),
  })
  .loose();

export const vedtakSchema = z
  .object({
    tilskudd: z.array(
      z
        .object({
          vedtakResultat: z.enum(VedtakResultat, { error: "Vedtaksresultat er påkrevd" }),
          belop: z.number().int("Beløp må være et heltall").nullable(),
          kommentarVedtaksbrev: z.string().nullable(),
          kommentarIntern: z.string().nullable(),
        })
        .loose()
        .superRefine((tilskudd, ctx) => {
          if (tilskudd.vedtakResultat === VedtakResultat.INNVILGELSE && tilskudd.belop === null) {
            ctx.addIssue({
              code: "custom",
              path: ["belop"],
              message: "Beløp må være et heltall",
            });
          }
          if (
            tilskudd.vedtakResultat === VedtakResultat.AVSLAG &&
            !tilskudd.kommentarVedtaksbrev?.trim()
          ) {
            ctx.addIssue({
              code: "custom",
              path: ["kommentarVedtaksbrev"],
              message: "Kommentar til deltaker må fylles ut ved avslag",
            });
          }
        }),
    ),
  })
  .loose();
