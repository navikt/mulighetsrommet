import { useParams } from "react-router";

type RequiredParams<Key extends string = string> = {
  readonly [key in Key]: string;
};

export const useRequiredParams = <Params extends string>(
  requiredParamNames: Params[],
): RequiredParams<(typeof requiredParamNames)[number]> => {
  const routeParams = useParams();

  for (const paramName of requiredParamNames) {
    const parameter = routeParams[paramName];
    if (!parameter) {
      throw new Error(
        `This component should not be rendered on a route which does not have the ${paramName} parameter`,
      );
    }
  }

  return routeParams as RequiredParams<(typeof requiredParamNames)[number]>;
};
