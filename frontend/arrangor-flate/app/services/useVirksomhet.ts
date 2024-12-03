import { useSearchParams } from "react-router";

const useVirksomhet = () => {
  const [searchParams] = useSearchParams();

  return searchParams.get("virksomhet");
};

export default useVirksomhet;
