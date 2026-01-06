import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import z from "zod";
import { avtaleDetaljerSchema, toUtdanningslopDbo, validateAvtaledetaljer } from "./avtaledetaljer";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";
import {
  AmoKategorisering,
  AmoKategoriseringRequest,
  AmoKurstype,
  AvtaleArrangorKontaktperson,
  AvtaleDto,
  NavAnsattDto,
  Personopplysning,
  PrismodellType,
} from "@tiltaksadministrasjon/api-client";

export const PrismodellSchema = z.object({
  prismodeller: z
    .object({
      id: z.uuid().optional(),
      prisbetingelser: z.string().nullable(),
      type: z.enum(PrismodellType, { error: "Du må velge en prismodell" }),
      satser: z.array(
        z.object({
          gjelderFra: z.string().nullable(),
          gjelderTil: z.string().nullable(),
          pris: z.number().nullable(),
          valuta: z.string(),
        }),
      ),
    })
    .array(),
});

export type PrismodellValues = z.infer<typeof PrismodellSchema>;

export const VeilederinformasjonSchema = z.object({
  beskrivelse: z.string().nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navAndreEnheter: z.string().array(),
});

export const VeilederinformasjonStepSchema = z.object({
  veilederinformasjon: VeilederinformasjonSchema,
});

export type VeilederinformasjonValues = z.infer<typeof VeilederinformasjonStepSchema>;

export const PersonopplysningerSchema = z.object({
  personvern: z.object({
    personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
    personopplysninger: z.enum(Personopplysning).array(),
  }),
});

export type PersonvernValues = z.infer<typeof PersonopplysningerSchema>;

export const avtaleFormSchema = avtaleDetaljerSchema
  .extend(PrismodellSchema.shape)
  .extend(PersonopplysningerSchema.shape)
  .extend(VeilederinformasjonStepSchema.shape)
  .superRefine((data, ctx) => {
    validateAvtaledetaljer(ctx, data);
  });

export type AvtaleFormInput = z.input<typeof avtaleFormSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleFormSchema>;

export function amoKategoriseringRequest(
  amoKategorisering: AmoKategorisering | null,
): AmoKategoriseringRequest | null {
  switch (amoKategorisering?.kurstype) {
    case "BRANSJE_OG_YRKESRETTET":
      return {
        kurstype: AmoKurstype.BRANSJE_OG_YRKESRETTET,
        bransje: amoKategorisering.bransje,
        sertifiseringer: amoKategorisering.sertifiseringer,
        forerkort: amoKategorisering.forerkort,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: null,
      };
    case "NORSKOPPLAERING":
      return {
        kurstype: AmoKurstype.NORSKOPPLAERING,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: amoKategorisering.norskprove,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case "GRUNNLEGGENDE_FERDIGHETER":
      return {
        kurstype: AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case "FORBEREDENDE_OPPLAERING_FOR_VOKSNE":
      return {
        kurstype: AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        innholdElementer: null,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case "STUDIESPESIALISERING":
      return {
        kurstype: AmoKurstype.STUDIESPESIALISERING,
        innholdElementer: null,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };

    case undefined:
      return null;
  }
}

export function defaultAvtaleData(
  ansatt: NavAnsattDto,
  avtale?: Partial<AvtaleDto>,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    detaljer: {
      administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
      navn: avtale?.navn,
      avtaletype: avtale?.avtaletype,
      arrangor: {
        hovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
        underenheter: !avtale?.arrangor?.underenheter
          ? []
          : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
        kontaktpersoner:
          avtale?.arrangor?.kontaktpersoner.map((p: AvtaleArrangorKontaktperson) => p.id) ?? [],
      },
      startDato: avtale?.startDato,
      sluttDato: avtale?.sluttDato ?? null,
      sakarkivNummer: avtale?.sakarkivNummer ?? null,
      tiltakskode: avtale?.tiltakstype?.tiltakskode,
      amoKategorisering: amoKategoriseringRequest(avtale?.amoKategorisering ?? null),
      opsjonsmodell: {
        type: avtale?.opsjonsmodell?.type,
        opsjonMaksVarighet: avtale?.opsjonsmodell?.opsjonMaksVarighet,
        customOpsjonsmodellNavn: avtale?.opsjonsmodell?.customOpsjonsmodellNavn,
      },
      utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    },
    veilederinformasjon: {
      navRegioner: navRegioner,
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      beskrivelse: avtale?.beskrivelse ?? null,
      faneinnhold: avtale?.faneinnhold ?? null,
    },
    personvern: {
      personvernBekreftet: avtale?.personvernBekreftet,
      personopplysninger: avtale?.personopplysninger ?? [],
    },
    prismodeller: avtale?.prismodeller?.map((prismodell) => ({
      id: prismodell?.id,
      type: prismodell?.type as PrismodellType | undefined,
      satser: prismodell?.satser ?? [],
      prisbetingelser:
        prismodell && "prisbetingelser" in prismodell ? (prismodell.prisbetingelser ?? null) : null,
    })) ?? [
      {
        id: undefined,
        type: undefined,
        satser: [],
        prisbetingelser: null,
      },
    ],
  };
}
