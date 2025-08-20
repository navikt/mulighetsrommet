import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import {
  ArrangorKontaktperson,
  AvtaleDto,
  AvtaltSatsDto,
  NavAnsatt,
  Personopplysning,
  Prismodell,
  PrismodellDto,
} from "@mr/api-client-v2";
import z from "zod";
import {
  avtaleDetaljerSchema,
  arrangorSchema,
  validateArrangor,
  validateAvtaledetaljer,
  toUtdanningslopDbo,
} from "./avtaledetaljer";
import { okonomiSchema } from "./okonomi";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";

export const RedaksjoneltInnholdSchema = z.object({
  beskrivelse: z
    .string({ error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navAndreEnheter: z.string().array(),
});

export const PersonopplysningerSchema = z.object({
  personvernBekreftet: z.boolean({ error: "Du må ta stilling til personvern" }),
  personopplysninger: z.enum(Personopplysning).array(),
});

export const avtaleFormSchema = avtaleDetaljerSchema
  .extend(arrangorSchema.shape)
  .extend(okonomiSchema.shape)
  .extend(PersonopplysningerSchema.shape)
  .extend(RedaksjoneltInnholdSchema.shape)
  .superRefine((data, ctx) => {
    validateArrangor(ctx, data);
    validateAvtaledetaljer(ctx, data);
  });

export type AvtaleFormInput = z.input<typeof avtaleFormSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleFormSchema>;

export function defaultAvtaleData(
  ansatt: NavAnsatt,
  avtale?: AvtaleDto,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    tiltakskode: avtale?.tiltakstype.tiltakskode,
    navRegioner: navRegioner,
    navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
    navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
    administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
    navn: avtale?.navn ?? "",
    avtaletype: avtale?.avtaletype,
    arrangorHovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
    arrangorUnderenheter: !avtale?.arrangor?.underenheter
      ? []
      : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
    arrangorKontaktpersoner:
      avtale?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    startDato: avtale?.startDato ? avtale.startDato : undefined,
    sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    sakarkivNummer: avtale?.sakarkivNummer,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    personvernBekreftet: avtale?.personvernBekreftet,
    personopplysninger: avtale?.personopplysninger ?? [],
    amoKategorisering: avtale?.amoKategorisering ?? null,
    opsjonsmodell: {
      type: avtale?.opsjonsmodell.type,
      opsjonMaksVarighet: avtale?.opsjonsmodell.opsjonMaksVarighet,
      customOpsjonsmodellNavn: avtale?.opsjonsmodell.customOpsjonsmodellNavn,
    },
    utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    prismodell: avtale?.prismodell.type as Prismodell,
    satser: avtale?.prismodell ? satser(avtale.prismodell) : [],
    prisbetingelser:
      avtale?.prismodell && "prisbetingelser" in avtale.prismodell
        ? (avtale.prismodell.prisbetingelser ?? undefined)
        : undefined,
  };
}

function satser(prismodell: PrismodellDto): AvtaltSatsDto[] {
  switch (prismodell.type) {
    case "ANNEN_AVTALT_PRIS":
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return [];
    case "AVTALT_PRIS_PER_MANEDSVERK":
    case "AVTALT_PRIS_PER_UKESVERK":
      return prismodell.satser;
  }
}
