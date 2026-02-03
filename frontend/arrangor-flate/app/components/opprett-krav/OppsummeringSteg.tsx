import { FieldError, LabeledDataElement, OpprettKravVedleggSteg } from "@api-client";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  Box,
  Button,
  Checkbox,
  CheckboxGroup,
  FileObject,
  FileUpload,
  Heading,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { Form } from "react-router";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { tekster } from "~/tekster";
import { errorAt } from "~/utils/validering";
import { Definisjonsliste, LabeledDataElementList } from "../common/Definisjonsliste";
import { addDuration, formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";

interface OppsummeringStepProps {
  innsendingsInformasjon: Array<LabeledDataElement>;
  formState: OpprettKravFormState;
  vedleggInfo: OpprettKravVedleggSteg;
  errors: FieldError[];
  goToPreviousStep: () => void;
}

export default function OppsummeringStep({
  innsendingsInformasjon,
  formState,
  vedleggInfo,
  errors,
  goToPreviousStep,
}: OppsummeringStepProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const acceptedFiles = formState.files.filter((f) => !f.error);

  useEffect(() => {
    if (fileInputRef.current && acceptedFiles.length > 0) {
      const dataTransfer = new DataTransfer();
      acceptedFiles.forEach((fileObj: FileObject) => {
        dataTransfer.items.add(fileObj.file);
      });
      fileInputRef.current.files = dataTransfer.files;
    }
  }, [acceptedFiles]);

  return (
    <>
      <Heading level="2" size="large">
        Oppsummering
      </Heading>
      <Box>
        <LabeledDataElementList title="Innsendingsinformasjon" entries={innsendingsInformasjon} />
        <Separator />
        <Definisjonsliste
          title="Utbetaling"
          definitions={[
            {
              key: "Utbetalingsperiode",
              value: formaterPeriode({
                start: formState.periodeStart!,
                slutt: formaterDato(
                  formState.periodeInklusiv
                    ? addDuration(formState.periodeSlutt!, { days: 1 })!
                    : formState.periodeSlutt!,
                )!,
              }),
            },
            { key: "Kontonummer", value: formState.kontonummer },
            { key: "KID-nummer", value: formState.kid },
            { key: "Beløp", value: formState.belop },
          ]}
        />
        <Separator />
        <Form method="post" encType="multipart/form-data">
          <input name="intent" value="submit" hidden readOnly />
          <input name="periodeStart" defaultValue={formState.periodeStart} hidden readOnly />
          <input name="periodeSlutt" defaultValue={formState.periodeSlutt} hidden readOnly />
          <input
            name="periodeInklusiv"
            defaultValue={String(formState.periodeInklusiv)}
            hidden
            readOnly
          />
          <input name="belop" defaultValue={formState.belop} hidden readOnly />
          <input name="kid" defaultValue={formState.kid || ""} hidden readOnly />
          <input name="tilsagnId" defaultValue={formState.tilsagnId} hidden readOnly />
          <input
            name="minAntallVedlegg"
            defaultValue={vedleggInfo.minAntallVedlegg}
            hidden
            readOnly
          />
          <input
            ref={fileInputRef}
            type="file"
            name="vedlegg"
            multiple
            style={{ display: "none" }}
          />

          <Box marginBlock="space-0 space-32">
            <Heading level="3" size="medium" spacing>
              Vedlegg
            </Heading>
            <Heading level="4" size="xsmall">
              {`Vedlegg (${acceptedFiles.length})`}
            </Heading>
            {acceptedFiles.length > 0 && (
              <VStack gap="space-8" marginBlock="space-4" align="start">
                <VStack as="ul" gap="space-8">
                  {acceptedFiles.map((file, index) => (
                    <FileUpload.Item as="li" key={index} file={file.file} />
                  ))}
                </VStack>
              </VStack>
            )}
            <Separator />
            <CheckboxGroup error={errorAt("/bekreftelse", errors)} legend="Bekreftelse">
              <Checkbox name="bekreftelse" value="bekreftet" id="bekreftelse">
                {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
              </Checkbox>
            </CheckboxGroup>
          </Box>
          <HStack gap="space-4">
            <Button type="button" variant="tertiary" onClick={goToPreviousStep}>
              Tilbake
            </Button>
            <Button type="submit">Bekreft og send inn</Button>
          </HStack>
        </Form>
      </Box>
    </>
  );
}
