import { Button, ErrorSummary, GuidePanel, Heading, HStack, VStack } from "@navikt/ds-react";
import {
  ActionFunction,
  Form,
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import {
  ArrangorflateService,
  ArrangorflateTilsagnDto,
  FieldError,
  Tilskuddstype,
} from "api-client";
import { commitSession, destroySession, getSession } from "~/sessions.server";
import { apiHeaders } from "~/auth/auth.server";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useEffect, useRef } from "react";
import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { FileUploader } from "~/components/fileUploader/FileUploader";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { pathByOrgnr } from "~/utils/navigation";
import { Separator } from "~/components/common/Separator";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 3 av 4: Vedlegg - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Last opp vedlegg for utbetalingskravet",
    },
  ];
};

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  tilsagn: ArrangorflateTilsagnDto;
  periodeStart: string;
  periodeSlutt: string;
  belop: number;
  kontonummer: string;
  kid?: string;
};

interface ActionData {
  errors?: FieldError[];
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingid } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  if (!gjennomforingid) {
    throw new Error("Mangler gjennomføring id");
  }

  const session = await getSession(request.headers.get("Cookie"));

  let tilsagnId: string | undefined;
  let periodeStart: string | undefined;
  let periodeSlutt: string | undefined;
  let belop: number | undefined;
  let kontonummer: string | undefined;
  let kid: string | undefined;
  if (
    session.get("orgnr") === orgnr &&
    session.get("tilskuddstype") === Tilskuddstype.TILTAK_INVESTERINGER &&
    session.get("gjennomforingId") === gjennomforingid
  ) {
    tilsagnId = session.get("tilsagnId");
    periodeStart = session.get("periodeStart");
    periodeSlutt = session.get("periodeSlutt");
    belop = Number(session.get("belop"));
    kontonummer = session.get("kontonummer");
    kid = session.get("kid");
  }

  if (!gjennomforingid || !tilsagnId || !periodeStart || !periodeSlutt || !belop || !kontonummer)
    throw new Error("Formdata mangler");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id: tilsagnId },
    headers: await apiHeaders(request),
  });
  if (error) {
    throw problemDetailResponse(error);
  }

  return {
    orgnr,
    gjennomforingId: gjennomforingid,
    tilsagn,
    periodeStart,
    periodeSlutt,
    belop,
    kontonummer,
    kid,
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

  const vedlegg = formData.getAll("vedlegg") as File[];

  if (vedlegg.length < 1) {
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
    return redirect(
      `${pathByOrgnr(orgnr!).opprettKrav.investering.oppsummering(gjennomforingId!)}`,
    );
  }
};

export default function OpprettKravOppsummering() {
  const { orgnr, gjennomforingId } = useLoaderData<LoaderData>();
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
        <GuidePanel className="">
          Du må laste opp vedlegg som dokumenterer de faktiske kostnadene dere har hatt for
          investeringer
        </GuidePanel>
        <Form method="post" encType="multipart/form-data">
          <VStack gap="6">
            <VStack gap="4">
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
            <HStack gap="4">
              <Button
                as={ReactRouterLink}
                type="button"
                variant="tertiary"
                to={pathByOrgnr(orgnr).opprettKrav.investering.utbetaling(gjennomforingId)}
              >
                Tilbake
              </Button>
              <Button type="submit">Nexte</Button>
            </HStack>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
