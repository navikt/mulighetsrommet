import { z } from "zod";
import {
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
} from "@tiltaksadministrasjon/api-client";

export const gjennomforingDetaljerSchema = z.object({}).loose();

export const gjennomforingVeilederinfoSchema = z.object({}).loose();

export const gjennomforingDetaljerWizardSchema = z
  .object({
    navn: z.string().min(1, "Navn er påkrevd"),
    startDato: z.string({ error: "Du må sette en startdato" }).min(1, "Du må sette en startdato"),
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
  })
  .loose();

export const gjennomforingVeilederinfoWizardSchema = z
  .object({
    veilederinformasjon: z.object({
      navRegioner: z.string().array().min(1, "Du må velge minst én Nav-region"),
      navKontorer: z.string().array(),
      navAndreEnheter: z.string().array(),
    }),
  })
  .loose();
