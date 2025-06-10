import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Button,
  ErrorSummary,
  FileObject,
  Heading,
  HStack,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { FieldError } from "api-client";
import { useRef } from "react";
import {
  ActionFunction,
  MetaFunction,
  useActionData,
  Link as ReactRouterLink,
  Form,
  redirect,
  LoaderFunction,
  useLoaderData,
} from "react-router";
import { FileUploader } from "../components/fileUploader/FileUploader";
import { internalNavigation } from "../internal-navigation";
import { errorAt } from "~/utils/validering";
import { commitSession, getSession } from "~/sessions.server";

export const meta: MetaFunction = () => {
  return [
    { title: "Manuell innsending" },
    { name: "description", content: "Manuell innsending av krav om utbetaling" },
  ];
};

type LoaderData = {
  sessionOrgnr: string;
  sessionVedlegg?: File[];
  sessionBeskrivelse?: string;
};

interface ActionData {
  errors?: FieldError[];
}

const MIN_BESKRIVELSE_LENGTH = 10;
const MAX_BESKRIVELSE_LENGTH = 500;

const uploadHandler: FileUploadHandler = async (fileUpload: FileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

export const loader: LoaderFunction = async ({ request }): Promise<LoaderData> => {
  const session = await getSession(request.headers.get("Cookie"));

  const sessionOrgnr = session.get("orgnr");
  if (!sessionOrgnr) throw new Error("Mangler orgnr");

  const sessionBeskrivelse = session.get("beskrivelse");

  return {
    sessionOrgnr,
    sessionBeskrivelse,
  };
};

export const action: ActionFunction = async ({ request }) => {
  const session = await getSession(request.headers.get("Cookie"));
  const formData = await parseFormData(
    request,
    {
      maxFileSize: 10000000, // 10MB
    },
    uploadHandler,
  );
  const errors: FieldError[] = [];
  const vedlegg = formData.getAll("vedlegg").filter((v) => v !== "") as File[];
  const beskrivelse = formData.get("beskrivelse")?.toString();
  const orgnr = session.get("orgnr");
  if (!orgnr) throw new Error("Mangler orgnr");

  if (!beskrivelse) {
    errors.push({
      pointer: "/beskrivelse",
      detail: "Du må fylle ut beskrivelsen",
    });
  }
  if (vedlegg.length === 0) {
    errors.push({
      pointer: "/vedlegg",
      detail: "Du må laste opp minst ett vedlegg",
    });
  }
  if (beskrivelse && beskrivelse.length < MIN_BESKRIVELSE_LENGTH) {
    errors.push({
      pointer: "/beskrivelse",
      detail: `Beskrivelse må være minst ${MIN_BESKRIVELSE_LENGTH} tegn`,
    });
  }

  if (beskrivelse && beskrivelse.length > MAX_BESKRIVELSE_LENGTH) {
    errors.push({
      pointer: "/beskrivelse",
      detail: `Beskrivelse kan være maks ${MAX_BESKRIVELSE_LENGTH} tegn`,
    });
  }

  if (errors.length > 0) {
    return { errors };
  } else {
    session.set("beskrivelse", beskrivelse);
    return redirect(internalNavigation(orgnr).opprettKravUtbetaling, {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
};

export default function ManuellUtbetalingForm() {
  const { sessionOrgnr, sessionVedlegg, sessionBeskrivelse } = useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const defaultFiles = sessionVedlegg?.map((v: File) => {
    return {
      file: v,
      error: false,
    } as FileObject;
  });

  return (
    <>
      <Heading level="3" spacing size="medium">
        Vedlegg
      </Heading>
      <Form method="post">
        <VStack gap="4">
          <FileUploader
            error={errorAt("/vedlegg", data?.errors)}
            maxFiles={10}
            maxSizeMB={3}
            maxSizeBytes={3 * 1024 * 1024}
            id="vedlegg"
            defaultFiles={defaultFiles}
          />
          <Textarea
            label="Beskrivelse"
            description="Her kan du spesifisere hva utbetalingen gjelder"
            name="beskrivelse"
            error={errorAt("/beskrivelse", data?.errors)}
            id="beskrivelse"
            defaultValue={sessionBeskrivelse}
            minLength={10}
            maxLength={500}
          />

          {data?.errors && data.errors.length > 0 && (
            <ErrorSummary ref={errorSummaryRef}>
              {data.errors.map((error: FieldError) => {
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
          <HStack>
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(sessionOrgnr).opprettKravTilsagn}
            >
              Tilbake
            </Button>
            <Button type="submit">Neste</Button>
          </HStack>
        </VStack>
      </Form>
    </>
  );
}
