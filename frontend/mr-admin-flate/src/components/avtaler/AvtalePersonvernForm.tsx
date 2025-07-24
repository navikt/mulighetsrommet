import {
  Alert,
  BodyShort,
  Checkbox,
  GuidePanel,
  HelpText,
  HStack,
  Label,
  Link,
  Radio,
  VStack,
} from "@navikt/ds-react";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { useFormContext } from "react-hook-form";
import { Separator } from "@/components/detaljside/Metadata";
import { AvtaleFormValues } from "@/schemas/avtale";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { EmbeddedTiltakstype, PersonopplysningData } from "@mr/api-client-v2";

interface Props {
  tiltakstype?: EmbeddedTiltakstype;
}

export function AvtalePersonvernForm({ tiltakstype }: Props) {
  const { register, setValue, watch } = useFormContext<AvtaleFormValues>();
  const { data: personopplysninger } = usePersonopplysninger();

  const watchPersonopplysninger = watch("personopplysninger");

  if (!tiltakstype) {
    return <Alert variant="info">Tiltakstype må velges før personvern kan redigeres.</Alert>;
  }

  function PersonopplysningCheckboxList(props: {
    label: string;
    description: string;
    personopplysninger: PersonopplysningData[];
  }) {
    return (
      <VStack>
        <Label size="small">{props.label}</Label>
        <BodyShort spacing size="small" textColor="subtle">
          {props.description}
        </BodyShort>
        <Checkbox
          checked={watchPersonopplysninger.length === props.personopplysninger.length}
          onChange={() => {
            if (watchPersonopplysninger.length < props.personopplysninger.length) {
              setValue(
                "personopplysninger",
                props.personopplysninger.map((p) => p.personopplysning),
              );
            } else {
              setValue("personopplysninger", []);
            }
          }}
          size="small"
        >
          Velg alle
        </Checkbox>
        <Separator />
        {props.personopplysninger?.map((p: PersonopplysningData) => (
          <HStack align="start" gap="1" key={p.personopplysning}>
            <Checkbox
              checked={watchPersonopplysninger.includes(p.personopplysning)}
              id={p.personopplysning}
              onChange={() =>
                setValue(
                  "personopplysninger",
                  addOrRemove(watchPersonopplysninger, p.personopplysning),
                )
              }
              size="small"
            >
              <span className="max-w-[65ch] inline-block"> {p.tittel}</span>
            </Checkbox>
            {p.hjelpetekst && <HelpText>{p.hjelpetekst}</HelpText>}
          </HStack>
        ))}
      </VStack>
    );
  }

  return (
    <VStack gap="4">
      <GuidePanel poster>
        Huk av de personopplysningene som er avtalt i databehandleravtalen. Nav tiltaksenhet/fylke
        er ansvarlig for at listen er i samsvar med gjeldende databehandleravtale.
      </GuidePanel>
      <HStack wrap gap="10">
        <VStack gap="5">
          {personopplysninger && (
            <PersonopplysningCheckboxList
              label="Personopplysninger om deltager"
              description="Huk av de personopplysningene som kan behandles i denne avtalen."
              personopplysninger={personopplysninger}
            />
          )}
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
          <Radio size="small" value={true} id={"bekreft-personopplysninger"}>
            Bekreft og vis hvilke personopplysninger som kan behandles til veileder
          </Radio>
        </VStack>
      </ControlledRadioGroup>
    </VStack>
  );
}
