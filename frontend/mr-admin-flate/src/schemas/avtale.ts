import { FaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import { ArrangorKontaktperson, AvtaleDto, NavAnsatt, Personopplysning } from "@mr/api-client-v2";
import z from "zod";
import {
  avtaleDetaljerSchema,
  arrangorSchema,
  validateArrangor,
  validateAvtaledetaljer,
  toUtdanningslopDbo,
} from "./avtaledetaljer";
import { okonomiSchema, validateOkonomi } from "./okonomi";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { DeepPartial } from "react-hook-form";

export const RedaksjoneltInnholdSchema = z.object({
  beskrivelse: z
    .string({ required_error: "En avtale trenger en beskrivelse i det redaksjonelle innholdet" })
    .nullable(),
  faneinnhold: FaneinnholdSchema.nullable(),
  navRegioner: z.string().array().nonempty({ message: "Du må velge minst én region" }),
  navKontorer: z.string().array(),
  navAndreEnheter: z.string().array(),
});

export const PersonopplysningerSchema = z.object({
  personvernBekreftet: z.boolean({ required_error: "Du må ta stilling til personvern" }),
  personopplysninger: z.nativeEnum(Personopplysning).array(),
});

export const avtaleFormSchema = avtaleDetaljerSchema
  .merge(arrangorSchema)
  .merge(okonomiSchema)
  .merge(PersonopplysningerSchema)
  .merge(RedaksjoneltInnholdSchema)
  .superRefine((data, ctx) => {
    validateArrangor(ctx, data);
    validateAvtaledetaljer(ctx, data);
    validateOkonomi(ctx, data);
  });

export type AvtaleFormInput = z.input<typeof avtaleFormSchema>;
export type AvtaleFormValues = z.infer<typeof avtaleFormSchema>;

export function defaultAvtaleData(
  ansatt: NavAnsatt,
  avtale?: AvtaleDto,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    tiltakstype: avtale?.tiltakstype,
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
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    personvernBekreftet: avtale?.personvernBekreftet,
    personopplysninger: avtale?.personopplysninger ?? [],
    amoKategorisering: avtale?.amoKategorisering ?? null,
    opsjonsmodell: avtale?.opsjonsmodell,
    utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    prismodell: avtale?.prismodell ?? null,
    satser: avtale?.satser ?? [],
  };
}
