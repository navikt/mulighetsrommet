import {
  Alert,
  BodyShort,
  Checkbox,
  GuidePanel,
  HGrid,
  HStack,
  Label,
  Radio,
  VStack,
} from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import styles from "./AvtalePersonvernForm.module.scss";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { personopplysningToTekst } from "@/utils/Utils";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { useEffect } from "react";
import { addOrRemove } from "mulighetsrommet-frontend-common/utils/utils";

interface Props {
  tiltakstypeId?: string;
}

export function AvtalePersonvernForm({ tiltakstypeId }: Props) {
  const { register, setValue, watch } = useFormContext<InferredAvtaleSchema>();
  const { data: tiltakstype } = useTiltakstype(tiltakstypeId);

  const watchPersonopplysninger = watch("personopplysninger");
  useEffect(() => {
    if (watchPersonopplysninger.length === 0 && tiltakstype) {
      setValue("personopplysninger", tiltakstype.personopplysninger.ALLTID);
    }
  }, [watchPersonopplysninger, tiltakstype]);

  if (!tiltakstypeId) {
    return (
      <div className={styles.container}>
        <Alert variant="info">Tiltakstype må velges før personvern kan redigeres.</Alert>
      </div>
    );
  }

  return (
    <VStack gap="4" className={styles.container}>
      <GuidePanel poster className={styles.guide_panel}>
        Huk av de personopplysningene som er avtalt i databehandleravtalen. NAV tiltaksenhet/fylke
        er ansvarlig for at listen er i samsvar med gjeldende databehandleravtale.
      </GuidePanel>
      <HGrid columns={2}>
        <VStack>
          <Label size="small">Opplysninger om brukeren som alltid kan/må behandles</Label>
          <BodyShort size="small" textColor="subtle">
            Fjern avhukingen hvis noen av opplysningene ikke er relevante for denne avtalen.
          </BodyShort>
          {tiltakstype?.personopplysninger?.ALLTID.map((p) => (
            <Checkbox
              checked={watchPersonopplysninger.includes(p)}
              onChange={() =>
                setValue("personopplysninger", addOrRemove(watchPersonopplysninger, p))
              }
              size="small"
              key={p}
            >
              {personopplysningToTekst(p)}
            </Checkbox>
          ))}
        </VStack>
        <VStack justify="space-between">
          <VStack>
            <Label size="small">
              Opplysninger om brukeren som ofte er nødvendig og relevant å behandle
            </Label>
            <BodyShort size="small" textColor="subtle">
              Huk av for de opplysningene som er avtalt i databehandleravtalen.
            </BodyShort>
            {tiltakstype?.personopplysninger?.OFTE.map((p) => (
              <Checkbox
                checked={watchPersonopplysninger.includes(p)}
                onChange={() =>
                  setValue("personopplysninger", addOrRemove(watchPersonopplysninger, p))
                }
                size="small"
                key={p}
              >
                {personopplysningToTekst(p)}
              </Checkbox>
            ))}
          </VStack>
          <VStack>
            <Label size="small">
              Opplysninger om brukeren som sjelden eller i helt spesielle tilfeller er nødvendig og
              relevant å behandle
            </Label>
            <BodyShort size="small" textColor="subtle">
              Huk av for de opplysningene som er avtalt i databehandleravtalen.
            </BodyShort>
            {tiltakstype?.personopplysninger?.SJELDEN.map((p) => (
              <Checkbox
                checked={watchPersonopplysninger.includes(p)}
                onChange={() =>
                  setValue("personopplysninger", addOrRemove(watchPersonopplysninger, p))
                }
                size="small"
                key={p}
              >
                {personopplysningToTekst(p)}
              </Checkbox>
            ))}
          </VStack>
          <BodyShort size="small">
            *Se egne retningslinjer om dette i veileder for arbeidsrettet brukeroppfølging pkt. 4.4.
          </BodyShort>
        </VStack>
      </HGrid>
      <ControlledRadioGroup
        size="small"
        legend=""
        hideLegend
        {...register("personvernBekreftet")}
      >
        <HStack align="center" justify="start" gap="4">
          <Radio size="small" value={false}>
            Hvilke personopplysninger som kan behandles er uavklært og kan ikke vises til veileder
          </Radio>
          <Radio size="small" value={true}>
            Bekreft og vis hvilke personveropplysninger som kan behandles til veileder
          </Radio>
        </HStack>
      </ControlledRadioGroup>
    </VStack>
  );
}
