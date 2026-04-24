import { z } from "zod";
import {
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
} from "@tiltaksadministrasjon/api-client";
import { VeilederinformasjonSchema } from "@/schemas/avtale";

export const gjennomforingDetaljerSchema = z
  .object({
    navn: z.string().min(1, "Navn er påkrevd"),
    startDato: z.string({ error: "Du må sette en startdato" }).min(1, "Du må sette en startdato"),
    sluttDato: z.string({ error: "Du må sette en startdato" }).nullable(),
    antallPlasser: z
      .number({ error: "Du må legge inn antall plasser" })
      .int()
      .positive("Antall plasser må være større enn 0"),
    administratorer: z.string().array().min(1, "Du må velge minst én administrator"),
    oppstart: z.enum(GjennomforingOppstartstype, { error: "Oppstartstype må være satt" }),
    pameldingType: z.enum(GjennomforingPameldingType, { error: "Påmeldingstype må være satt" }),
    prismodellId: z
      .string({ error: "Du må velge en prismodell fra avtalen" })
      .min(1, "Du må velge en prismodell fra avtalen"),
    arrangorId: z.string({ error: "Du må velge arrangør" }).min(1, "Du må velge arrangør"),
    arrangorKontaktpersoner: z.string().array(),
    oppmoteSted: z.string().nullable(),
    deltidsprosent: z.number(),
    tilgjengeligForArrangorDato: z.string().nullable(),
    estimertVentetid: z
      .object({
        verdi: z.number().nullable(),
        enhet: z.string().nullable(),
      })
      .nullable(),
  })
  .loose();

export type GjennomforingDetaljerInputValues = z.input<typeof gjennomforingDetaljerSchema>;
export type GjennomforingDetaljerOutputValues = z.infer<typeof gjennomforingDetaljerSchema>;

export const gjennomforingVeilederinfoSchema = z
  .object({
    veilederinformasjon: VeilederinformasjonSchema.extend({
      kontaktpersoner: z
        .object({
          navIdent: z.string(),
          beskrivelse: z.string().nullable(),
        })
        .array(),
    }),
  })
  .loose();

export type GjennomforingVeilederinfoInputValues = z.input<typeof gjennomforingVeilederinfoSchema>;
export type GjennomforingVeilederinfoOutputValues = z.infer<typeof gjennomforingVeilederinfoSchema>;

export const gjennomforingWizardSchema = gjennomforingDetaljerSchema.extend(
  gjennomforingVeilederinfoSchema.shape,
);

export type GjennomforingFormValues = z.infer<typeof gjennomforingWizardSchema>;
