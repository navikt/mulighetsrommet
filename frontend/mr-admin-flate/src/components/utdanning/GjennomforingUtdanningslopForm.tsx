import { Alert, Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { AvtaleDto, GjennomforingRequest, Tiltakskode } from "@tiltaksadministrasjon/api-client";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingUtdanningslopForm({ avtale }: Props) {
  const { register } = useFormContext<GjennomforingRequest>();

  switch (avtale.tiltakstype.tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.ARBEIDSRETTET_REHABILITERING:
    case Tiltakskode.AVKLARING:
    case Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK:
    case Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.JOBBKLUBB:
    case Tiltakskode.OPPFOLGING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
    case Tiltakskode.ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.STUDIESPESIALISERING:
    case Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV:
    case Tiltakskode.HOYERE_UTDANNING:
    case Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING:
      return null;

    case Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.FAG_OG_YRKESOPPLAERING:
  }

  if (!avtale.utdanningslop) {
    return (
      <Alert variant="warning">{avtaletekster.utdanning.utdanningsprogramManglerForAvtale}</Alert>
    );
  }

  return (
    <>
      <Select size="small" readOnly label={avtaletekster.utdanning.utdanningsprogram.label}>
        <option>{avtale.utdanningslop.utdanningsprogram.navn}</option>
      </Select>
      <ControlledMultiSelect
        size="small"
        label={avtaletekster.utdanning.laerefag.label}
        placeholder={avtaletekster.utdanning.laerefag.velg}
        {...register("utdanningslop.utdanninger")}
        options={avtale.utdanningslop.utdanninger.map((utdanning) => {
          return { value: utdanning.id, label: utdanning.navn };
        })}
      />
    </>
  );
}
