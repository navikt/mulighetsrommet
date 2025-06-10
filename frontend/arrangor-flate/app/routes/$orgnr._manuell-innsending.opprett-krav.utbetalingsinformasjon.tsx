import { Button, Heading, HStack, TextField, VStack } from "@navikt/ds-react";
import { ArrangorflateService, FieldError } from "api-client";
import {
  LoaderFunction,
  MetaFunction,
  useLoaderData,
  useRevalidator,
  Link as ReactRouterLink,
  Form,
  ActionFunctionArgs,
  redirect,
  useActionData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { KontonummerInput } from "~/components/KontonummerInput";
import { problemDetailResponse, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "../internal-navigation";
import { errorAt } from "~/utils/validering";
import { commitSession, getSession } from "~/sessions.server";

type LoaderData = {
  kontonummer?: string;
  sessionBelop?: string;
  sessionKid?: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Manuell innsending" },
    { name: "description", content: "Manuell innsending av krav om utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

  const session = await getSession(request.headers.get("Cookie"));
  const sessionBelop = session.get("belop");
  const sessionKid = session.get("kid");

  const [{ data: kontonummer, error: kontonummerError }] = await Promise.all([
    ArrangorflateService.getKontonummer({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
  ]);

  if (kontonummerError) {
    throw problemDetailResponse(kontonummerError);
  }

  return { kontonummer, sessionBelop, sessionKid };
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
    return redirect(internalNavigation(orgnr).opprettKravOppsummering, {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

export default function ManuellUtbetalingForm() {
  const data = useActionData<ActionData>();
  const { kontonummer, sessionBelop, sessionKid } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();
  const revalidator = useRevalidator();

  return (
    <>
      <Heading size="large" spacing level="2">
        Utbetalingsinformasjon
      </Heading>
      <Form method="post">
        <VStack gap="4">
          <TextField
            label="Beløp til utbetaling"
            defaultValue={sessionBelop}
            error={errorAt("/belop", data?.errors)}
            htmlSize={35}
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
          <HStack>
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(orgnr).opprettKravVedlegg}
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
