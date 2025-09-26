import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import {
  ArrangorKontaktperson,
  AvtaleDto,
  AvtaltSatsDto,
  Personopplysning,
  PrismodellDto,
} from "@mr/api-client-v2";
import z from "zod";
import { avtaleDetaljerSchema, toUtdanningslopDbo, validateAvtaledetaljer } from "./avtaledetaljer";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";
import { NavAnsattDto, Prismodell } from "@tiltaksadministrasjon/api-client";

export const PrismodellSchema = z.object({
  prisbetingelser: z.string().optional(),
  type: z.enum(Prismodell, { error: "Du må velge en prismodell" }),
  satser: z.array(
    z.object({
      gjelderFra: z.string().nullable(),
      pris: z.number().nullable(),
      valuta: z.string(),
    }),
  ),
});

export const PrismodellStepSchema = z.object({
  prismodell: PrismodellSchema,
});

export type PrismodellValues = z.infer<typeof PrismodellSchema>;

export const RedaksjoneltInnholdSchema = z.object({
  beskrivelse: z
    .string({ error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
});

export const VeilederinformasjonSchema = z.object({
  redaksjoneltInnhold: RedaksjoneltInnholdSchema,
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navAndreEnheter: z.string().array(),
});
export const VeilederinformasjonStepSchema = z.object({
  veilederinformasjon: VeilederinformasjonSchema,
});

export const PersonopplysningerSchema = z.object({
  personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
  personopplysninger: z.enum(Personopplysning).array(),
});

export const PersonopplysningerStepSchema = z.object({
  personvern: PersonopplysningerSchema,
});

export const avtaleSchema = z
  .object({
    detaljer: avtaleDetaljerSchema,
    prismodell: PrismodellSchema,
    personvern: PersonopplysningerSchema,
    veilederinformasjon: VeilederinformasjonSchema,
  })
  .superRefine((data, ctx) => {
    validateAvtaledetaljer(ctx, data.detaljer);
  });

export type AvtaleFormInput = z.input<typeof avtaleSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleSchema>;

export function defaultAvtaleData(
  ansatt: NavAnsattDto,
  avtale?: Partial<AvtaleDto>,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    detaljer: {
      navn: avtale?.navn ?? "",
      sakarkivNummer: avtale?.sakarkivNummer,
      tiltakskode: avtale?.tiltakstype?.tiltakskode,
      administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
      avtaletype: avtale?.avtaletype,
      arrangor: {
        hovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
        underenheter: !avtale?.arrangor?.underenheter
          ? []
          : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
        kontaktpersoner:
          avtale?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
      },
      startDato: avtale?.startDato,
      sluttDato: avtale?.sluttDato,
      amoKategorisering: avtale?.amoKategorisering ?? null,
      opsjonsmodell: avtale?.opsjonsmodell,
      utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    },
    prismodell: {
      type: avtale?.prismodell?.type as Prismodell | undefined,
      satser: avtale?.prismodell ? satser(avtale.prismodell) : [],
      prisbetingelser:
        avtale?.prismodell && "prisbetingelser" in avtale.prismodell
          ? (avtale.prismodell.prisbetingelser ?? undefined)
          : undefined,
    },
    personvern: {
      personvernBekreftet: avtale?.personvernBekreftet,
      personopplysninger: avtale?.personopplysninger ?? [],
    },
    veilederinformasjon: {
      navRegioner: navRegioner,
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      redaksjoneltInnhold: {
        beskrivelse: avtale?.beskrivelse ?? null,
        faneinnhold: avtale?.faneinnhold ?? null,
      },
    },
  };
}

function satser(prismodell: PrismodellDto): AvtaltSatsDto[] {
  switch (prismodell.type) {
    case "ANNEN_AVTALT_PRIS":
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return [];
    case "AVTALT_PRIS_PER_MANEDSVERK":
    case "AVTALT_PRIS_PER_UKESVERK":
    case "AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER":
      return prismodell.satser;
  }
}
