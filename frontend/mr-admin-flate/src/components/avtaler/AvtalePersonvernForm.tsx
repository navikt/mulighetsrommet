import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import {
  Alert,
  BodyShort,
  Checkbox,
  GuidePanel,
  HStack,
  HelpText,
  Label,
  Link,
  Radio,
  VStack,
} from "@navikt/ds-react";
import { PersonopplysningMedBeskrivelse } from "mulighetsrommet-api-client";
import { addOrRemove } from "mulighetsrommet-frontend-common/utils/utils";
import { useFormContext } from "react-hook-form";
import { Separator } from "../detaljside/Metadata";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import styles from "./AvtalePersonvernForm.module.scss";

interface Props {
  tiltakstypeId?: string;
}

export function AvtalePersonvernForm({ tiltakstypeId }: Props) {
  const { register, setValue, watch } = useFormContext<InferredAvtaleSchema>();
  const { data: tiltakstype } = useTiltakstype(tiltakstypeId);

  const watchPersonopplysninger = watch("personopplysninger");

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
    personopplysninger?: PersonopplysningMedBeskrivelse[];
  }) {
    return (
      <VStack>
        <Label size="small">{props.label}</Label>
        <BodyShort spacing size="small" textColor="subtle">
          {props.description}
        </BodyShort>
        {props.personopplysninger?.map((p: PersonopplysningMedBeskrivelse) => (
          <HStack align="start" gap="1" key={p.personopplysning}>
            <Checkbox
              checked={!!watchPersonopplysninger.includes(p.personopplysning)}
              onChange={() =>
                setValue(
                  "personopplysninger",
                  addOrRemove(watchPersonopplysninger, p.personopplysning),
                )
              }
              size="small"
            >
              <span className={styles.max_length_text}> {p.beskrivelse}</span>
            </Checkbox>
            {p.hjelpetekst && <HelpText>{p.hjelpetekst}</HelpText>}
          </HStack>
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
      <HStack wrap gap="10">
        <VStack gap="5">
          <PersonopplysningCheckboxList
            label="Følgende personopplysninger om deltager kan behandles i denne avtalen"
            description="Fjern avhukingen hvis noen av opplysningene ikke er relevante for denne avtalen."
            personopplysninger={tiltakstype?.personopplysninger}
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
      </HStack>
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
