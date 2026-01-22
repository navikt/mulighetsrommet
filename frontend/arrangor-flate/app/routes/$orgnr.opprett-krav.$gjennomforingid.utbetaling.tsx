import { ErrorSummary, Heading, TextField, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  FieldError,
  OpprettKravUtbetalingsinformasjon,
  OpprettKravVeiviserSteg,
} from "api-client";
import {
  ActionFunctionArgs,
  Form,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
  useRevalidator,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";
import { errorAt, problemDetailResponse } from "~/utils/validering";
import { commitSession, getSession } from "~/sessions.server";
import {
  getOrgnrGjennomforingIdFrom,
  pathBySteg,
  useGjennomforingIdFromUrl,
  useOrgnrFromUrl,
} from "~/utils/navigation";
import { useEffect, useRef } from "react";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid";
import {
  nesteStegFieldName,
  OpprettKravVeiviserButtons,
} from "~/components/OpprettKravVeiviserButtons";

type LoaderData = {
  innsendingsinformasjon: OpprettKravUtbetalingsinformasjon;
  sessionBelop?: string;
  sessionKid?: string;
};

export const meta: MetaFunction = ({ matches }) => {
  return [
    { title: getStepTitle(matches) },
    {
      name: "description",
      content: "Fyll ut betalingsinformasjon for å opprette et krav om utbetaling",
    },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);
  const session = await getSession(request.headers.get("Cookie"));
  let sessionBelop: string | undefined;
  let sessionKid: string | undefined;
  if (session.get("orgnr") === orgnr && session.get("gjennomforingId") === gjennomforingId) {
    sessionBelop = session.get("belop");
    sessionKid = session.get("kid");
  }

  const [{ data, error: kontonummerError }] = await Promise.all([
    ArrangorflateService.getOpprettKravUtbetalingsinformasjon({
      path: { orgnr, gjennomforingId },
      headers: await apiHeaders(request),
    }),
  ]);

  if (kontonummerError) {
    throw problemDetailResponse(kontonummerError);
  }

  return { innsendingsinformasjon: data, sessionBelop, sessionKid };
};

interface ActionData {
  errors?: FieldError[];
}

export async function action({ request }: ActionFunctionArgs) {
  const errors: FieldError[] = [];
  const session = await getSession(request.headers.get("Cookie"));
  const formData = await request.formData();

  const orgnr = session.get("orgnr");
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const gjennomforingId = session.get("gjennomforingId");
  if (!gjennomforingId) {
    throw new Error("Mangler gjennomføring id");
  }

  const belop = formData.get("belop")?.toString();
  const kontonummer = formData.get("kontonummer")?.toString();
  const kid = formData.get("kid")?.toString();

  if (!belop) {
    errors.push({
      pointer: "/belop",
      detail: "Du må fylle ut beløp",
    });
  }
  if (!kontonummer) {
    errors.push({
      pointer: "/kontonummer",
      detail: "Fant ikke kontonummer",
    });
  }

  if (errors.length > 0) {
    return { errors };
  } else {
    const nesteSteg = formData.get(nesteStegFieldName) as OpprettKravVeiviserSteg;
    session.set("belop", belop);
    session.set("kid", kid);
    session.set("kontonummer", kontonummer);
    return redirect(pathBySteg(nesteSteg, orgnr, gjennomforingId), {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

export default function OpprettKravUtbetaling() {
  const data = useActionData<ActionData>();
  const { innsendingsinformasjon, sessionBelop, sessionKid } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();
  const gjennomforingId = useGjennomforingIdFromUrl();
  const revalidator = useRevalidator();
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  return (
    <>
      <Form method="post">
        <VStack gap="4">
          <Heading size="large" level="3">
            Utbetalingsinformasjon
          </Heading>
          <TextField
            label="Beløp til utbetaling"
            description="Oppgi samlet beløp som skal faktureres Nav for denne utbetalingsperioden"
            defaultValue={sessionBelop}
            error={errorAt("/belop", data?.errors)}
            type="number"
            htmlSize={15}
            size="small"
            name="belop"
            id="belop"
          />
          <VStack gap="4">
            <KontonummerInput
              error={errorAt("/kontonummer", data?.errors)}
              kontonummer={innsendingsinformasjon.kontonummer}
              onClick={() => revalidator.revalidate()}
            />
            <TextField
              label="KID-nummer for utbetaling (valgfritt)"
              defaultValue={sessionKid}
              size="small"
              name="kid"
              htmlSize={35}
              maxLength={25}
              id="kid"
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
            navigering={innsendingsinformasjon.navigering}
            orgnr={orgnr}
            gjennomforingId={gjennomforingId}
            submitNeste
          />
        </VStack>
      </Form>
    </>
  );
}
