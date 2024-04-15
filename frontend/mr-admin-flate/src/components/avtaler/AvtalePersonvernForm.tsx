import {
  Alert,
  BodyShort,
  Checkbox,
  GuidePanel,
  HGrid,
  Label,
  Link,
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
import { Separator } from "../detaljside/Metadata";
import { Personopplysning } from "mulighetsrommet-api-client";

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

  function PersonopplysningCheckboxList(props: {
    label: string;
    description: string;
    personopplysninger?: Personopplysning[];
  }) {
    return (
      <VStack>
        <Label size="small">{props.label}</Label>
        <BodyShort size="small" textColor="subtle">
          {props.description}
        </BodyShort>
        {props.personopplysninger?.map((p: Personopplysning) => (
          <Checkbox
            checked={watchPersonopplysninger.includes(p)}
            onChange={() => setValue("personopplysninger", addOrRemove(watchPersonopplysninger, p))}
            size="small"
            key={p}
          >
            {personopplysningToTekst(p)}
          </Checkbox>
        ))}
      </VStack>
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
          <PersonopplysningCheckboxList
            label="Opplysninger om brukeren som alltid kan/må behandles"
            description="Fjern avhukingen hvis noen av opplysningene ikke er relevante for denne avtalen."
            personopplysninger={tiltakstype?.personopplysninger?.ALLTID}
          />
        </VStack>
        <VStack justify="space-between">
          <PersonopplysningCheckboxList
            label="Opplysninger om brukeren som ofte er nødvendig og relevant å behandle"
            description="Huk av for de opplysningene som er avtalt i databehandleravtalen."
            personopplysninger={tiltakstype?.personopplysninger?.OFTE}
          />
          <PersonopplysningCheckboxList
            label="Opplysninger om brukeren som sjelden eller i helt spesielle tilfeller er nødvendig og relevant å behandle"
            description="Huk av for de opplysningene som er avtalt i databehandleravtalen."
            personopplysninger={tiltakstype?.personopplysninger?.SJELDEN}
          />
          <BodyShort size="small">
            *Se egne retningslinjer om dette i{" "}
            <Link
              target="_blank"
              href="https://navno.sharepoint.com/sites/fag-og-ytelser-veileder-for-arbeidsrettet-brukeroppfolging/SitePages/Arbeidsrettede-tiltak.aspx"
            >
              veileder for arbeidsrettet brukeroppfølging
            </Link>{" "}
            pkt. 4.3.
          </BodyShort>
        </VStack>
      </HGrid>
      <Separator />
      <ControlledRadioGroup
        size="small"
        legend="Ta stillingen til om personvernopplysningene stemmer eller om avtalen ikke er ferdig signert"
        hideLegend
        {...register("personvernBekreftet")}
      >
        <VStack align="start" justify="start" gap="2">
          <Radio size="small" value={false}>
            Hvilke personopplysninger som kan behandles er uavklart og kan ikke vises til veileder
          </Radio>
          <Radio size="small" value={true}>
            Bekreft og vis hvilke personopplysninger som kan behandles til veileder
          </Radio>
        </VStack>
      </ControlledRadioGroup>
    </VStack>
  );
}
