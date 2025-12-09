import {
  Button,
  Checkbox,
  CheckboxGroup,
  ErrorSummary,
  FileObject,
  Heading,
  HStack,
  VStack,
} from "@navikt/ds-react";
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
import { ArrangorflateService, FieldError, OpprettKravOppsummering } from "api-client";
import { destroySession, getSession } from "~/sessions.server";
import { apiHeaders } from "~/auth/auth.server";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useEffect, useRef, useState } from "react";
import { LabeledDataElementList } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { addFilesTo } from "~/components/fileUploader/FileUploader";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { getOrgnrGjennomforingIdFrom, pathTo } from "~/utils/navigation";
import { Separator } from "~/components/common/Separator";
import { VedleggUtlisting } from "~/components/VedleggUtlisting";
import { useFileStorage } from "~/hooks/useFileStorage";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid._tilskudd";

export const meta: MetaFunction = ({ matches }) => {
  return [
    { title: getStepTitle(matches) },
    {
      name: "description",
      content: "Oppsummering av krav om utbetaling og last opp vedlegg",
    },
  ];
};

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  oppsummering: OpprettKravOppsummering;
};

interface ActionData {
  errors?: FieldError[];
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  const session = await getSession(request.headers.get("Cookie"));

  let tilsagnId: string | undefined;
  let periodeStart: string | undefined;
  let periodeSlutt: string | undefined;
  let periodeInklusiv: boolean | undefined;
  let belop: number | undefined;
  let kontonummer: string | undefined;
  let kidNummer: string | undefined;

  if (session.get("orgnr") === orgnr && session.get("gjennomforingId") === gjennomforingId) {
    tilsagnId = session.get("tilsagnId");
    periodeStart = session.get("periodeStart");
    periodeSlutt = session.get("periodeSlutt");
    periodeInklusiv = session.get("periodeInklusiv") == "true" || false;
    belop = Number(session.get("belop"));
    kontonummer = session.get("kontonummer");
    kidNummer = session.get("kid");
  }
  if (!tilsagnId || !periodeStart || !periodeSlutt || !belop || !kontonummer) {
    throw new Error("Formdata mangler");
  }
  const { data: oppsummering, error } = await ArrangorflateService.getOpprettKravOppsummering({
    path: { orgnr, gjennomforingId },
    headers: await apiHeaders(request),
    body: {
      periodeStart,
      periodeSlutt,
      periodeInklusiv: periodeInklusiv ?? null,
      kidNummer: kidNummer ?? null,
      belop,
    },
  });
  if (error) {
    throw problemDetailResponse(error);
  }

  return {
    orgnr,
    gjennomforingId,
    oppsummering,
  };
};

const uploadHandler: FileUploadHandler = async (fileUpload: FileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

export const action: ActionFunction = async ({ request, params }) => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

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
  const bekreftelse = formData.get("bekreftelse");
  const belop = Number(formData.get("belop"));
  const periodeStart = formData.get("periodeStart");
  const periodeSlutt = formData.get("periodeSlutt");
  const kidNummer = formData.get("kidNummer");
  const tilsagnId = session.get("tilsagnId");

  const minAntallVedleggField = formData.get("minAntallVedlegg");
  const minAntallVedlegg =
    typeof minAntallVedleggField === "string" ? parseInt(minAntallVedleggField) : 1;
  if (vedlegg.length < minAntallVedlegg) {
    errors.push({
      pointer: "/vedlegg",
      detail: "Du må legge ved vedlegg",
    });
  }

  if (!bekreftelse) {
    errors.push({
      pointer: "/bekreftelse",
      detail: "Du må bekrefte at opplysningene er korrekte",
    });
  }

  if (errors.length > 0) {
    return { errors };
  }

  const { error, data } = await ArrangorflateService.postOpprettKrav({
    path: { orgnr: orgnr!, gjennomforingId: gjennomforingId },
    body: {
      belop: belop!,
      tilsagnId: tilsagnId!,
      periodeStart: periodeStart!.toString(),
      periodeSlutt: periodeSlutt!.toString(),
      kidNummer: kidNummer?.toString() || null,
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
    return redirect(`${pathTo.kvittering(orgnr, data.id)}`, {
      headers: {
        "Set-Cookie": await destroySession(session),
      },
    });
  }
};

export default function OpprettKrav() {
  const { orgnr, gjennomforingId, oppsummering } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const storage = useFileStorage();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<FileObject[]>([]);
  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    // Ved last
    if (!files.length) {
      addFilesTo(fileInputRef, setFiles, storage);
    }
  });

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <VStack gap="6">
        <LabeledDataElementList
          title="Innsendingsinformasjon"
          entries={oppsummering.innsendingsInformasjon}
        />
        <Separator />
        <LabeledDataElementList title="Utbetaling" entries={oppsummering.utbetalingInformasjon} />
        <Separator />
        <Form method="post" encType="multipart/form-data">
          <input
            name="periodeStart"
            defaultValue={oppsummering.innsendingsData.periode.start}
            readOnly
            hidden
          />
          <input
            name="periodeSlutt"
            defaultValue={oppsummering.innsendingsData.periode.slutt}
            readOnly
            hidden
          />
          <input
            name="kidNummer"
            defaultValue={oppsummering.innsendingsData.kidNummer ?? undefined}
            readOnly
            hidden
          />
          <input
            name="minAntallVedlegg"
            defaultValue={oppsummering.innsendingsData.minAntallVedlegg}
            readOnly
            hidden
          />
          <input name="belop" defaultValue={oppsummering.innsendingsData.belop} readOnly hidden />
          <VStack gap="6">
            <VedleggUtlisting files={files} fileInputRef={fileInputRef} />
            <Separator />
            <CheckboxGroup error={errorAt("/bekreftelse", data?.errors)} legend={"Bekreftelse"}>
              <Checkbox
                name="bekreftelse"
                value="bekreftet"
                id="bekreftelse"
                error={errorAt("/bekreftelse", data?.errors) !== undefined}
              >
                {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
              </Checkbox>
            </CheckboxGroup>
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
                to={pathTo.opprettKrav.vedlegg(orgnr, gjennomforingId)}
              >
                Tilbake
              </Button>
              <Button type="submit">Bekreft og send inn</Button>
            </HStack>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
