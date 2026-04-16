import type { ServiceEntity } from '../serviceModel'
import { ServiceCard } from './ServiceCard'

interface ServiceGalleryProps {
  services: ServiceEntity[]
  onOpen: (service: ServiceEntity) => void
}

export function ServiceGallery({ services, onOpen }: ServiceGalleryProps) {
  return (
    <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {services.map((service) => (
        <ServiceCard key={service.id} service={service} onOpen={onOpen} />
      ))}
    </div>
  )
}
