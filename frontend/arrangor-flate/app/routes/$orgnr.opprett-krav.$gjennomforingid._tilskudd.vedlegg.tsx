import { ErrorSummary, GuidePanel, Heading, VStack } from "@navikt/ds-react";
import {
  ActionFunction,
  Form,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import {
  ArrangorflateService,
  OpprettKravVedlegg,
  FieldError,
  OpprettKravVedleggGuidePanelType,
  OpprettKravVeiviserSteg,
} from "api-client";
import { getSession } from "~/sessions.server";
import { apiHeaders } from "~/auth/auth.server";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useEffect, useRef } from "react";
import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { FileUploader } from "~/components/fileUploader/FileUploader";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { getOrgnrGjennomforingIdFrom, pathBySteg } from "~/utils/navigation";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid._tilskudd";
import {
  OpprettKravVeiviserButtons,
  nesteStegFieldName,
} from "~/components/OpprettKravVeiviserButtons";

const minAntallVedleggFieldName = "minAntallVedlegg";

export const meta: MetaFunction = ({ matches }) => {
  return [
    { title: getStepTitle(matches) },
    {
      name: "description",
      content: "Last opp vedlegg for utbetalingskravet",
    },
  ];
};

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  vedleggInfo: OpprettKravVedlegg;
};

interface ActionData {
  errors?: FieldError[];
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  const { data: vedleggInfo, error } = await ArrangorflateService.getOpprettKravVedlegg({
    path: { orgnr, gjennomforingId },
    headers: await apiHeaders(request),
  });
  if (error) {
    throw problemDetailResponse(error);
  }

  return {
    orgnr,
    gjennomforingId,
    vedleggInfo,
  };
};

const uploadHandler: FileUploadHandler = async (fileUpload: FileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

export const action: ActionFunction = async ({ request }) => {
  const formData = await parseFormData(
    request,
    {
      maxFileSize: 10000000, // 10MB
    },
    uploadHandler,
  );
  const session = await getSession(request.headers.get("Cookie"));
  const errors: FieldError[] = [];

  const minAntallVedleggField = formData.get(minAntallVedleggFieldName);
  const minAntallVedlegg =
    typeof minAntallVedleggField === "string" ? parseInt(minAntallVedleggField) : 1;
  const vedlegg = formData.getAll("vedlegg") as File[];

  if (vedlegg.length < minAntallVedlegg) {
    errors.push({
      pointer: "/vedlegg",
      detail: "Du må legge ved vedlegg",
    });
  }

  const orgnr = session.get("orgnr")!;
  const gjennomforingId = session.get("gjennomforingId")!;

  if (errors.length > 0) {
    return { errors };
  }

  const nesteSteg = formData.get(nesteStegFieldName) as OpprettKravVeiviserSteg;
  const redirectPath = pathBySteg(nesteSteg, orgnr, gjennomforingId);

  if (vedlegg.length === 0) {
    return redirect(redirectPath);
  }

  const { error } = await ArrangorflateService.scanVedlegg({
    body: {
      vedlegg: vedlegg,
    },
    headers: await apiHeaders(request),
  });

  if (error) {
    if (isValidationError(error)) {
      return { errors: error.errors };
    } else {
      throw problemDetailResponse(error);
    }
  } else {
    return redirect(redirectPath);
  }
};

export default function OpprettKravVedleggSteg() {
  const { orgnr, gjennomforingId, vedleggInfo } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  return (
    <>
      <Heading level="2" spacing size="large">
        Vedlegg
      </Heading>
      <VStack gap="6">
        <GuidePanelVedlegg type={vedleggInfo.guidePanel} />
        <Form method="post" encType="multipart/form-data">
          <VStack gap="6">
            <VStack gap="4">
              <input
                name={minAntallVedleggFieldName}
                defaultValue={vedleggInfo.minAntallVedlegg}
                readOnly
                hidden
              />
              <FileUploader
                fileStorage
                error={errorAt("/vedlegg", data?.errors)}
                maxFiles={10}
                maxSizeMB={3}
                maxSizeBytes={3 * 1024 * 1024}
                id="vedlegg"
              />
            </VStack>
            {hasError && (
              <ErrorSummary ref={errorSummaryRef}>
                {data.errors?.map((error: FieldError) => {
                  return (
                    <ErrorSummary.Item
                      href={`#${jsonPointerToFieldPath(error.pointer)}`}
                      key={jsonPointerToFieldPath(error.pointer)}
                    >
                      {error.detail}
                    </ErrorSummary.Item>
                  );
                })}
              </ErrorSummary>
            )}
            <OpprettKravVeiviserButtons
              navigering={vedleggInfo.navigering}
              orgnr={orgnr}
              gjennomforingId={gjennomforingId}
              submitNeste
            />
          </VStack>
        </Form>
      </VStack>
    </>
  );
}

interface GuidePanelVedleggProps {
  type: OpprettKravVedleggGuidePanelType | null;
}

function GuidePanelVedlegg({ type }: GuidePanelVedleggProps) {
  switch (type) {
    case OpprettKravVedleggGuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          Du må laste opp vedlegg som dokumenterer de faktiske kostnadene dere har hatt for
          investeringer
        </GuidePanel>
      );
    case OpprettKravVedleggGuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          Fakturering skal skje i henhold til prisbilag i avtalen og eventuelle presiseringer. Dere
          må sikre at opplysningene dere oppgir er korrekte. Det skal kun faktureres for faktisk
          medgått tid, eventuelt rundet av til nærmeste hele kvarter. Nav vil kunne gjennomføre
          kontroller og kreve innsyn for å verifisere at tjenesten og tilhørerende fakturering er i
          henhold til avtalen. Fakturavedleggsskjema kan lastes ned her{" "}
          <b>Oppfølging - fakturagrunnlag 2021 (excel)</b>
        </GuidePanel>
      );
    case OpprettKravVedleggGuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>Her kan du laste opp vedlegg som er relevante for utbetalingen</GuidePanel>
      );

    case null:
    default:
      return null;
  }
}
