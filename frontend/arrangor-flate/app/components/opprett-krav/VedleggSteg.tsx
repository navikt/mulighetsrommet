import {
  FieldError,
  OpprettKravVedleggSteg,
  OpprettKravVedleggStegGuidePanelType,
} from "@api-client";
import {
  BodyShort,
  FileObject,
  FileRejected,
  FileRejectionReason,
  FileUpload,
  GuidePanel,
  Heading,
  Link,
  VStack,
} from "@navikt/ds-react";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { errorAt } from "~/utils/validering";

interface VedleggStepProps {
  data: OpprettKravVedleggSteg;
  formState: OpprettKravFormState;
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
  errors: FieldError[];
}

export default function VedleggSteg({
  data,
  formState,
  updateFormState,
  errors,
}: VedleggStepProps) {
  const maxFiles = 10;
  const maxSizeMB = 10;
  const maxSizeBytes = 10 * 1024 * 1024;

  const acceptedFiles = formState.files.filter((file: FileObject) => !file.error);
  const rejectedFiles = formState.files.filter((f: FileObject): f is FileRejected => f.error);

  const fileErrors: Record<FileRejectionReason, string> = {
    fileType: "Filformatet støttes ikke",
    fileSize: `Filen er større enn ${maxSizeMB} MB`,
  };

  const removeFile = (fileToRemove: FileObject) => {
    const remainingFiles = formState.files.filter((file: FileObject) => file !== fileToRemove);
    updateFormState({ files: remainingFiles });
  };

  return (
    <>
      <Heading level="2" spacing size="large">
        Vedlegg
      </Heading>
      <VStack gap="space-24">
        <GuidePanelVedlegg type={data.guidePanel} />
        <FileUpload.Dropzone
          label="Last opp vedlegg"
          description={`Du kan laste opp PDF-filer. Maks ${maxFiles} filer. Maks størrelse ${maxSizeMB} MB per fil.`}
          accept=".pdf"
          id="vedlegg"
          error={errorAt("/vedlegg", errors)}
          maxSizeInBytes={maxSizeBytes}
          fileLimit={{ max: maxFiles, current: acceptedFiles.length }}
          onSelect={(newFiles: FileObject[]) => {
            updateFormState({ files: [...formState.files, ...newFiles] });
          }}
        />

        {acceptedFiles.length > 0 && (
          <VStack gap="space-8">
            <Heading level="4" size="xsmall">
              {`Vedlegg (${acceptedFiles.length})`}
            </Heading>
            <VStack as="ul" gap="space-8" align="start">
              {acceptedFiles.map((file, index) => (
                <FileUpload.Item
                  as="li"
                  key={index}
                  file={file.file}
                  button={{
                    action: "delete",
                    onClick: () => removeFile(file),
                  }}
                />
              ))}
            </VStack>
          </VStack>
        )}
        {rejectedFiles.length > 0 && (
          <VStack gap="space-8">
            <Heading level="4" size="xsmall">
              Vedlegg med feil
            </Heading>
            <VStack as="ul" gap="space-8" align="start">
              {rejectedFiles.map((rejected, index) => (
                <FileUpload.Item
                  as="li"
                  key={index}
                  file={rejected.file}
                  error={fileErrors[rejected.reasons[0] as FileRejectionReason]}
                  button={{
                    action: "delete",
                    onClick: () => removeFile(rejected),
                  }}
                />
              ))}
            </VStack>
          </VStack>
        )}
      </VStack>
    </>
  );
}

interface GuidePanelVedleggProps {
  type: OpprettKravVedleggStegGuidePanelType | null;
}

function GuidePanelVedlegg({ type }: GuidePanelVedleggProps) {
  switch (type) {
    case OpprettKravVedleggStegGuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          Du må laste opp vedlegg som dokumenterer de faktiske kostnadene dere har hatt for
          investeringer
        </GuidePanel>
      );
    case OpprettKravVedleggStegGuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          <BodyShort spacing>
            Fakturering skal skje i henhold til prisbilag i avtalen og eventuelle presiseringer.
            Dere må sikre at opplysningene dere oppgir er korrekte.
          </BodyShort>
          <BodyShort spacing>Det skal kun faktureres for faktisk medgått tid.</BodyShort>
          <BodyShort spacing>
            Nav vil kunne gjennomføre kontroller og kreve innsyn for å verifisere at tjenesten og
            tilhørerende fakturering er i henhold til avtalen.
          </BodyShort>
          <BodyShort>
            <Link
              inlineText
              target="_blank"
              href="https://www.nav.no/samarbeidspartner/faktura-tiltak/#fakturavedlegg"
            >
              Fakturavedleggsskjema
            </Link>{" "}
            eller tilsvarende dokumentasjon skal lastes opp under.
          </BodyShort>
        </GuidePanel>
      );
    case OpprettKravVedleggStegGuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>
          Her skal du laste opp faktura og eventuelt andre vedlegg som er relevante for utbetalingen
        </GuidePanel>
      );
    case null:
      return null;
  }
}
