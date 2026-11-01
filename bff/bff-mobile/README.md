# Mobile apps BFF (`bff-mobile`)

| | |
|---|---|
| Owning PRDs | [X03](../../spec/prd/06-experience/PRD-X03-mobile.md) |
| HTTP port (local) | `8204` |
| Deployable | `:bff:bff-mobile` |

Backend for the customer mobile apps: ID cards, mobile FNOL, claim tracking, payments, push.

A BFF composes bounded-context APIs for one client. It holds **no domain logic**: it never enforces business
rules, computes money, or decides state (CRC-ARCH-011). It may depend only on context `-api` modules and
platform libraries; the build and `MobileBffContractTest` enforce this.
