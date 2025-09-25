import { Button, ErrorSummary, Heading, HStack, TextField, VStack } from "@navikt/ds-react";
import { ArrangorflateService, FieldError, Tilskuddstype } from "api-client";
import {
  ActionFunctionArgs,
  Form,
  Link as ReactRouterLink,
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
import { pathByOrgnr, useGjennomforingIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { useEffect, useRef } from "react";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

type LoaderData = {
  kontonummer?: string;
  sessionBelop?: string;
  sessionKid?: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 2 av 3: Betalingsinformasjon - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Fyll ut betalingsinformasjon for å opprette et krav om utbetaling",
    },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const { gjennomforingid } = params;

  const session = await getSession(request.headers.get("Cookie"));
  let sessionBelop: string | undefined;
  let sessionKid: string | undefined;
  if (
    session.get("orgnr") === orgnr &&
    session.get("tilskuddstype") === Tilskuddstype.TILTAK_INVESTERINGER &&
    session.get("gjennomforingId") === gjennomforingid
  ) {
    sessionBelop = session.get("belop");
    sessionKid = session.get("kid");
  }

  const [{ data, error: kontonummerError }] = await Promise.all([
    ArrangorflateService.getKontonummer({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
  ]);

  if (kontonummerError) {
    throw problemDetailResponse(kontonummerError);
  }

  return {
    kontonummer: data.kontonummer,
    sessionBelop,
    sessionKid,
  };
};

interface ActionData {
  errors?: FieldError[];
}

export async function action({ request }: ActionFunctionArgs) {
  const errors: FieldError[] = [];
  const session = await getSession(request.headers.get("Cookie"));
  const formData = await request.formData();

  const orgnr = session.get("orgnr");
  if (!orgnr) throw new Error("Mangler orgnr");

  const gjennomforingId = session.get("gjennomforingId");
  if (!gjennomforingId) throw new Error("Mangler gjennomføring id");

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
    session.set("belop", belop);
    session.set("kid", kid);
    session.set("kontonummer", kontonummer);
    return redirect(pathByOrgnr(orgnr).opprettKrav.investering.vedlegg(gjennomforingId), {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

export default function OpprettKravUtbetaling() {
  const data = useActionData<ActionData>();
  const { kontonummer, sessionBelop, sessionKid } = useLoaderData<LoaderData>();
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
      <Heading size="large" spacing level="3">
        Utbetalingsinformasjon
      </Heading>
      <Form method="post">
        <VStack gap="4">
          <TextField
            label="Beløp til utbetaling"
            defaultValue={sessionBelop}
            error={errorAt("/belop", data?.errors)}
            inputMode="numeric"
            htmlSize={15}
            size="small"
            name="belop"
            id="belop"
          />
          <VStack gap="4">
            <KontonummerInput
              error={errorAt("/kontonummer", data?.errors)}
              kontonummer={kontonummer}
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
          <HStack gap="4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={pathByOrgnr(orgnr).opprettKrav.investering.innsendingsinformasjon(
                gjennomforingId,
              )}
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
