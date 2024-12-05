import { AvtalerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function avtaleLoader({ params }: LoaderFunctionArgs) {
  if (!params.avtaleId) throw Error("Fant ikke avtaleId i route");
  return await AvtalerService.getAvtale({ id: params.avtaleId });
}
