import {
  Alert,
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
import { VedleggUtlisting } from "~/components/VedleggUtlisting";
import { useFileStorage } from "~/hooks/useFileStorage";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

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
  // eslint-disable-next-line no-console
  console.log("Processing file upload:", {
    fieldName: fileUpload.fieldName,
    fileName: fileUpload.name,
    fileType: fileUpload.type,
    isPdf: fileUpload.name.endsWith(".pdf"),
  });

  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    try {
      const bytes = await fileUpload.bytes();
      // eslint-disable-next-line no-console
      console.log("Successfully read file bytes:", {
        fileName: fileUpload.name,
        size: bytes.length,
      });
      return new File([bytes], fileUpload.name, { type: fileUpload.type });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("Failed to read file bytes:", fileUpload.name, error);
      throw error;
    }
  } else {
    // eslint-disable-next-line no-console
    console.warn("File skipped (not a PDF or wrong field):", fileUpload.name);
  }
};

export const action: ActionFunction = async ({ request, params }) => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  let formData;
  try {
    formData = await parseFormData(
      request,
      {
        maxFileSize: 10000000, // 10MB
      },
      uploadHandler,
    );
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error("Failed to parse form data:", error);
    return {
      errors: [
        {
          pointer: "/vedlegg",
          detail: `Kunne ikke lese vedlegg fra skjemaet. Feilmelding: ${error instanceof Error ? error.message : String(error)}`,
        },
      ],
    };
  }

  // Log what was successfully parsed
  // eslint-disable-next-line no-console
  console.log("Form data parsed successfully:", {
    allKeys: Array.from(formData.keys()),
    vedleggCount: formData.getAll("vedlegg").length,
  });

  const session = await getSession(request.headers.get("Cookie"));
  const errors: FieldError[] = [];

  const vedlegg = formData.getAll("vedlegg") as File[];

  // eslint-disable-next-line no-console
  console.log("Vedlegg extracted from form:", {
    count: vedlegg.length,
    fileNames: vedlegg.map((f) => f.name),
    fileSizes: vedlegg.map((f) => f.size),
  });
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
      belop,
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
  const [filesLoading, setFilesLoading] = useState(true);
  const [filesLoadError, setFilesLoadError] = useState<string | null>(null);
  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  useEffect(() => {
    // Ved last - only run once on mount
    let isMounted = true;

    const loadFiles = async () => {
      if (files.length > 0) {
        // Files already loaded
        setFilesLoading(false);
        return;
      }

      try {
        setFilesLoading(true);
        setFilesLoadError(null);
        await addFilesTo(fileInputRef, setFiles, storage);

        if (isMounted) {
          setFilesLoading(false);
          // eslint-disable-next-line no-console
          console.log("Files loaded from IndexedDB successfully");
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error("Failed to load files from IndexedDB:", error);
        if (isMounted) {
          setFilesLoadError(
            error instanceof Error ? error.message : "Ukjent feil ved lasting av vedlegg",
          );
          setFilesLoading(false);
        }
      }
    };

    loadFiles();

    return () => {
      isMounted = false;
    };
  }, [files.length, storage]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    if (filesLoading) {
      e.preventDefault();
      // eslint-disable-next-line no-console
      console.warn("Form submission prevented: Files are still loading");
      return;
    }

    if (filesLoadError) {
      e.preventDefault();
      // eslint-disable-next-line no-console
      console.warn("Form submission prevented: File load error");
      return;
    }

    if (!fileInputRef.current?.files || fileInputRef.current.files.length === 0) {
      e.preventDefault();
      // eslint-disable-next-line no-console
      console.error("Form submission prevented: No files attached to file input", {
        filesInState: files.length,
        filesInInput: fileInputRef.current?.files?.length || 0,
      });
      return;
    }

    // eslint-disable-next-line no-console
    console.log("Form submitting with files:", {
      filesInState: files.length,
      filesInInput: fileInputRef.current.files.length,
      fileNames: Array.from(fileInputRef.current.files).map((f) => f.name),
      fileSizes: Array.from(fileInputRef.current.files).map((f) => f.size),
    });
  };

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
        <Form method="post" encType="multipart/form-data" onSubmit={handleSubmit}>
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
            {filesLoading && <Alert variant="info">Laster vedlegg...</Alert>}
            {filesLoadError && (
              <Alert variant="error">
                Kunne ikke laste vedlegg fra lokal lagring: {filesLoadError}
                <br />
                Vennligst gå tilbake og last opp vedleggene på nytt.
              </Alert>
            )}
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
              <Button
                type="submit"
                loading={filesLoading}
                disabled={filesLoading || !!filesLoadError}
              >
                Bekreft og send inn
              </Button>
            </HStack>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
