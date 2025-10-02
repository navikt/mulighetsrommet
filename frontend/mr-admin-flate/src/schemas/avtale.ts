import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import {
  ArrangorKontaktperson,
  Personopplysning,
  PrismodellType,
  AmoKategorisering,
} from "@mr/api-client-v2";
import z from "zod";
import { avtaleDetaljerSchema, toUtdanningslopDbo, validateAvtaledetaljer } from "./avtaledetaljer";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";
import { AvtaleDto, NavAnsattDto } from "@tiltaksadministrasjon/api-client";
import { slateFaneinnholdToPortableText } from "../components/portableText/helper";

export const PrismodellSchema = z.object({
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
});

export const PrismodellStepSchema = z.object({
  prismodell: PrismodellSchema,
});

export type PrismodellValues = z.infer<typeof PrismodellSchema>;

export const VeilederinformasjonSchema = z.object({
  beskrivelse: z
    .string({ error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
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
      sakarkivNummer: avtale?.sakarkivNummer ?? null,
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
      startDato: avtale?.startDato ?? null,
      sluttDato: avtale?.sluttDato ?? null,
      // TODO: fiks typer
      amoKategorisering: (avtale?.amoKategorisering as AmoKategorisering | undefined) ?? null,
      opsjonsmodell: avtale?.opsjonsmodell,
      utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    },
    prismodell: {
      type: avtale?.prismodell?.type as PrismodellType | undefined,
      satser: avtale?.prismodell?.satser ?? [],
      prisbetingelser:
        avtale?.prismodell && "prisbetingelser" in avtale.prismodell
          ? (avtale.prismodell.prisbetingelser ?? null)
          : null,
    },
    personvern: {
      personvernBekreftet: avtale?.personvernBekreftet,
      personopplysninger: avtale?.personopplysninger ?? [],
    },
    veilederinformasjon: {
      navRegioner: navRegioner,
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      beskrivelse: avtale?.beskrivelse ?? null,
      faneinnhold: slateFaneinnholdToPortableText(avtale?.faneinnhold),
    },
  };
}
