# Customer portal BFF (`bff-customer`)

| | |
|---|---|
| Owning PRDs | [X01](../../spec/prd/06-experience/PRD-X01-customer-portal.md) |
| HTTP port (local) | `8201` |
| Deployable | `:bff:bff-customer` |

Backend for the customer self-service portal: policies, billing, FNOL, claim tracking, preferences.

A BFF composes bounded-context APIs for one client. It holds **no domain logic**: it never enforces business
rules, computes money, or decides state (CRC-ARCH-011). It may depend only on context `-api` modules and
platform libraries; the build and `CustomerBffContractTest` enforce this.
