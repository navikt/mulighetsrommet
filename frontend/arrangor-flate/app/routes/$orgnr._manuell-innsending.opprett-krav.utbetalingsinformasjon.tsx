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

type LoaderData = {
  kontonummer?: string;
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

  const [{ data: kontonummer, error: kontonummerError }] = await Promise.all([
    ArrangorflateService.getKontonummer({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
  ]);

  if (kontonummerError) {
    throw problemDetailResponse(kontonummerError);
  }

  return { kontonummer };
};

interface ActionData {
  errors?: FieldError[];
}

export async function action({ request }: ActionFunctionArgs) {
  const errors: FieldError[] = [];

  const formData = await request.formData();

  const orgnr = formData.get("orgnr")?.toString();
  const belop = formData.get("belop")?.toString();
  const kontonummer = formData.get("kontonummer")?.toString();

  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

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
    return redirect(internalNavigation(orgnr).opprettKravOppsummering);
  }
}

export default function ManuellUtbetalingForm() {
  const data = useActionData<ActionData>();
  const { kontonummer } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();
  const revalidator = useRevalidator();

  return (
    <>
      <Heading size="large" spacing level="2">
        Utbetalingsinformasjon
      </Heading>
      <Form method="post">
        <VStack gap="4">
          <input type="hidden" name="orgnr" value={orgnr} />
          <TextField
            label="Beløp til utbetaling"
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
