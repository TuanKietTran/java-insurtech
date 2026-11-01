# Producer portal BFF (`bff-producer`)

| | |
|---|---|
| Owning PRDs | [X02](../../spec/prd/06-experience/PRD-X02-producer-portal.md) |
| HTTP port (local) | `8202` |
| Deployable | `:bff:bff-producer` |

Backend for the producer (agent/broker) portal: quoting, binding, servicing, book of business, commissions.

A BFF composes bounded-context APIs for one client. It holds **no domain logic**: it never enforces business
rules, computes money, or decides state (CRC-ARCH-011). It may depend only on context `-api` modules and
platform libraries; the build and `ProducerBffContractTest` enforce this.
