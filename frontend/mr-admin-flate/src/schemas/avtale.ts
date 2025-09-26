import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import {
  AmoKategorisering,
  ArrangorKontaktperson,
  Personopplysning,
  PrismodellType,
} from "@mr/api-client-v2";
import z from "zod";
import {
  arrangorSchema,
  avtaleDetaljerSchema,
  toUtdanningslopDbo,
  validateArrangor,
  validateAvtaledetaljer,
} from "./avtaledetaljer";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";
import { AvtaleDto, NavAnsattDto } from "@tiltaksadministrasjon/api-client";

export const PrismodellSchema = z.object({
  prisbetingelser: z.string().nullable(),
  prismodell: z.enum(PrismodellType, { error: "Du må velge en prismodell" }),
  satser: z.array(
    z.object({
      gjelderFra: z.string().nullable(),
      gjelderTil: z.string().nullable(),
      pris: z.number().nullable(),
      valuta: z.string(),
    }),
  ),
});

export type PrismodellValues = z.infer<typeof PrismodellSchema>;

export const RedaksjoneltInnholdSchema = z.object({
  beskrivelse: z
    .string({ error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navEnheterAndre: z.string().array(),
});

export const PersonopplysningerSchema = z.object({
  personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
  personopplysninger: z.enum(Personopplysning).array(),
});

export const avtaleFormSchema = avtaleDetaljerSchema
  .extend(arrangorSchema.shape)
  .extend(PrismodellSchema.shape)
  .extend(PersonopplysningerSchema.shape)
  .extend(RedaksjoneltInnholdSchema.shape)
  .superRefine((data, ctx) => {
    validateArrangor(ctx, data);
    validateAvtaledetaljer(ctx, data);
  });

export type AvtaleFormInput = z.input<typeof avtaleFormSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleFormSchema>;

export function defaultAvtaleData(
  ansatt: NavAnsattDto,
  avtale?: Partial<AvtaleDto>,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    tiltakskode: avtale?.tiltakstype?.tiltakskode,
    navRegioner: navRegioner,
    navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
    navEnheterAndre: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
    administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
    navn: avtale?.navn ?? "",
    avtaletype: avtale?.avtaletype,
    arrangorHovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
    arrangorUnderenheter: !avtale?.arrangor?.underenheter
      ? []
      : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
    arrangorKontaktpersoner:
      avtale?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    startDato: avtale?.startDato ?? null,
    sluttDato: avtale?.sluttDato ?? null,
    sakarkivNummer: avtale?.sakarkivNummer ?? null,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    personvernBekreftet: avtale?.personvernBekreftet,
    personopplysninger: avtale?.personopplysninger ?? [],
    // TODO: fiks typer
    amoKategorisering: (avtale?.amoKategorisering as AmoKategorisering | undefined) ?? null,
    opsjonsmodell: {
      type: avtale?.opsjonsmodell?.type,
      opsjonMaksVarighet: avtale?.opsjonsmodell?.opsjonMaksVarighet,
      customOpsjonsmodellNavn: avtale?.opsjonsmodell?.customOpsjonsmodellNavn,
    },
    utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    prismodell: avtale?.prismodell?.type as PrismodellType | undefined,
    satser: avtale?.prismodell?.satser ?? [],
    prisbetingelser:
      avtale?.prismodell && "prisbetingelser" in avtale.prismodell
        ? (avtale.prismodell.prisbetingelser ?? undefined)
        : undefined,
  };
}
