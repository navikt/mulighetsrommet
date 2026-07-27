import { ChevronDownIcon, FilterIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  filterTab: ReactNode;
  setFilterOpen: (filterOpen: boolean) => void;
  filterOpen: boolean;
}

export function FilterSidebar({ filterTab, setFilterOpen, filterOpen }: Props) {
  return (
    <VStack height="fit-content" gap="space-8" width="350px" className={`sticky top-2 z-1 `}>
      <Box asChild background="default">
        <Button
          size="small"
          data-color="neutral"
          variant="secondary"
          data-testid="filterbox"
          onClick={() => {
            setFilterOpen(!filterOpen);
          }}
          icon={
            <ChevronDownIcon
              aria-hidden
              className={`transition-transform ease-in-out duration-75 ${filterOpen ? "rotate-180" : "rotate-0"}`}
            />
          }
          iconPosition="right"
        >
          <HStack gap="space-8" align="center">
            <FilterIcon fontSize="1.3rem" aria-hidden />
            <Heading level="2" size="small">
              Filter
            </Heading>
          </HStack>
        </Button>
      </Box>
      <Box
        borderColor="neutral-subtle"
        borderWidth="1"
        borderRadius="8"
        maxHeight="85vh"
        paddingInline="space-16"
        paddingBlock="space-24"
        className={`overflow-y-auto ${filterOpen ? "" : "hidden"}`}
        background="default"
        shadow="dialog"
      >
        {filterTab}
      </Box>
    </VStack>
  );
}
